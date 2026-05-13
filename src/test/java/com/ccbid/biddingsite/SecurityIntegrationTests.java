package com.ccbid.biddingsite;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.ccbid.biddingsite.models.AccountRole;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.models.UserAccount;
import com.ccbid.biddingsite.repository.AuctioneerRepo;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.BidderRepo;
import com.ccbid.biddingsite.repository.ItemRepo;
import com.ccbid.biddingsite.repository.UserAccountRepo;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:security-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
class SecurityIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private BidderRepo bidderRepo;

    @Autowired
    private AuctioneerRepo auctioneerRepo;

    @Autowired
    private ItemRepo itemRepo;

    @Autowired
    private BidRepo bidRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bidRepo.deleteAll();
        itemRepo.deleteAll();
        bidderRepo.deleteAll();
        auctioneerRepo.deleteAll();
        userAccountRepo.deleteAll();

        mockMvc = webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void registerCreatesBidderAccountAndProfile() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "taylor-bidder",
                      "email": "taylor@example.com",
                      "password": "secret123",
                      "displayName": "Taylor",
                      "role": "BIDDER"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("taylor-bidder"))
            .andExpect(jsonPath("$.role").value("BIDDER"))
            .andExpect(jsonPath("$.profileId").value("taylor-bidder"));
    }

    @Test
    void loginReturnsRegisteredAccount() throws Exception {
        saveAccount("casey-auctioneer", "secret123", "casey@example.com", "Casey", AccountRole.AUCTIONEER);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "casey-auctioneer",
                      "password": "secret123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Casey"))
            .andExpect(jsonPath("$.role").value("AUCTIONEER"));
    }

    @Test
    void biddersEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/bidders/all"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void bidderCanReadBiddersEndpoint() throws Exception {
        saveAccount("bidder-user", "secret123", "bidder@example.com", "Bidder User", AccountRole.BIDDER);

        mockMvc.perform(get("/bidders/all")
                .with(httpBasic("bidder-user", "secret123")))
            .andExpect(status().isOk());
    }

    @Test
    void bidderCannotListAuctionItem() throws Exception {
        saveAccount("bidder-user", "secret123", "bidder@example.com", "Bidder User", AccountRole.BIDDER);

        mockMvc.perform(post("/bid/list")
                .with(httpBasic("bidder-user", "secret123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemId": "forbidden-item",
                      "itemName": "Camera",
                      "startingPrice": 100
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void auctioneerCanListAuctionItemWithAuthenticatedIdentity() throws Exception {
        saveAccount("auctioneer-user", "secret123", "auctioneer@example.com", "Alex", AccountRole.AUCTIONEER);

        mockMvc.perform(post("/bid/list")
                .with(httpBasic("auctioneer-user", "secret123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemId": "listed-by-auctioneer",
                      "itemName": "Laptop",
                      "startingPrice": 300,
                      "auctioneerId": "spoofed-id",
                      "auctioneerName": "Spoofed Name"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Listed listed-by-auctioneer")))
            .andExpect(content().string(containsString("auctioneer-user")));
    }

    @Test
    void bidderCannotSpoofAnotherBidderId() throws Exception {
        saveAccount("auctioneer-user", "secret123", "auctioneer@example.com", "Alex", AccountRole.AUCTIONEER);
        saveAccount("bidder-user", "secret123", "bidder@example.com", "Taylor", AccountRole.BIDDER);

        mockMvc.perform(post("/bid/list")
                .with(httpBasic("auctioneer-user", "secret123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemId": "item-1",
                      "itemName": "Laptop",
                      "startingPrice": 300
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/bid/item-1")
                .with(httpBasic("bidder-user", "secret123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bidderId": "someone-else",
                      "amount": 325
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Authenticated user does not match bidderId")));
    }

    private void saveAccount(String username,
                             String rawPassword,
                             String email,
                             String displayName,
                             AccountRole role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setEmail(email);
        account.setDisplayName(displayName);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setCreatedAt(Instant.now());
        userAccountRepo.save(account);

        if (role == AccountRole.BIDDER) {
            bidderRepo.save(new Bidder(username, displayName));
        }
        if (role == AccountRole.AUCTIONEER) {
            auctioneerRepo.save(new Auctioneer(username, displayName));
        }
    }
}
