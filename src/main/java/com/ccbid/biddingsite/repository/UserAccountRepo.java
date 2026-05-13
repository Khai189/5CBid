package com.ccbid.biddingsite.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ccbid.biddingsite.models.UserAccount;

public interface UserAccountRepo extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
}
