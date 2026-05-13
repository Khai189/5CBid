package com.ccbid.biddingsite.service;

import java.time.Instant;
import java.util.Locale;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccbid.biddingsite.dto.AuthLoginRequest;
import com.ccbid.biddingsite.dto.AuthResponse;
import com.ccbid.biddingsite.dto.AuthSessionResponse;
import com.ccbid.biddingsite.dto.RegisterRequest;
import com.ccbid.biddingsite.models.AccountRole;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.models.UserAccount;
import com.ccbid.biddingsite.repository.AuctioneerRepo;
import com.ccbid.biddingsite.repository.BidderRepo;
import com.ccbid.biddingsite.repository.UserAccountRepo;

@Service
public class AuthService {

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private BidderRepo bidderRepo;

    @Autowired
    private AuctioneerRepo auctioneerRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Value("${app.security.jwt-expiration-seconds:86400}")
    private long jwtExpirationSeconds;

    @Transactional
    public AuthSessionResponse register(RegisterRequest request) {
        String username = normalizeRequired(request.username(), "username");
        String email = normalizeEmail(request.email());
        String password = normalizeRequired(request.password(), "password");
        String displayName = normalizeDisplayName(request.displayName(), username);
        AccountRole role = parseSignupRole(request.role());

        if (userAccountRepo.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("Username " + username + " is already taken");
        }
        if (userAccountRepo.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email " + email + " is already registered");
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setDisplayName(displayName);
        account.setRole(role);
        account.setCreatedAt(Instant.now());

        UserAccount saved = userAccountRepo.save(account);
        ensureRoleProfile(saved);
        return toSession(saved);
    }

    @Transactional(readOnly = true)
    public AuthSessionResponse login(AuthLoginRequest request) {
        String username = normalizeRequired(request.username(), "username");
        String password = normalizeRequired(request.password(), "password");
        UserAccount account = getRequiredAccount(username);

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return toSession(account);
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(String username) {
        return toResponse(getRequiredAccount(username));
    }

    @Transactional(readOnly = true)
    public UserAccount getRequiredAccount(String username) {
        return userAccountRepo.findByUsernameIgnoreCase(normalizeRequired(username, "username"))
            .orElseThrow(() -> new IllegalStateException("Account " + username + " not found"));
    }

    private void ensureRoleProfile(UserAccount account) {
        if (account.getRole() == AccountRole.BIDDER && !bidderRepo.existsById(account.getUsername())) {
            bidderRepo.save(new Bidder(account.getUsername(), account.getDisplayName()));
        }
        if (account.getRole() == AccountRole.AUCTIONEER && !auctioneerRepo.existsById(account.getUsername())) {
            auctioneerRepo.save(new Auctioneer(account.getUsername(), account.getDisplayName()));
        }
    }

    private AuthResponse toResponse(UserAccount account) {
        return new AuthResponse(
            account.getUsername(),
            account.getEmail(),
            account.getDisplayName(),
            account.getRole().name(),
            account.getRole() == AccountRole.ADMIN ? null : account.getUsername()
        );
    }

    private AuthSessionResponse toSession(UserAccount account) {
        return new AuthSessionResponse(createToken(account), "Bearer", toResponse(account));
    }

    private String createToken(UserAccount account) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtExpirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("ccbid")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(account.getUsername())
            .claim("roles", List.of(account.getRole().name()))
            .claim("displayName", account.getDisplayName())
            .claim("email", account.getEmail())
            .claim("profileId", account.getRole() == AccountRole.ADMIN ? null : account.getUsername())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private AccountRole parseSignupRole(String rawRole) {
        String normalized = normalizeRequired(rawRole, "role").toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(normalized)) {
            throw new IllegalArgumentException("Public signup cannot create admin accounts");
        }
        try {
            return AccountRole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("role must be BIDDER or AUCTIONEER");
        }
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeRequired(email, "email").toLowerCase(Locale.ROOT);
        if (!normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
        return normalized;
    }

    private String normalizeDisplayName(String displayName, String username) {
        if (displayName == null || displayName.isBlank()) {
            return username;
        }
        return displayName.trim();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
