package com.ccbid.biddingsite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.ccbid.biddingsite.dto.RegisterRequest;
import com.ccbid.biddingsite.repository.AuctioneerRepo;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.BidderRepo;
import com.ccbid.biddingsite.repository.ItemRepo;
import com.ccbid.biddingsite.repository.UserAccountRepo;
import com.ccbid.biddingsite.service.AuthService;

import static org.hamcrest.Matchers.containsString;
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
    "spring.h2.console.enabled=false",
    "app.security.jwt-secret=test-jwt-secret-with-at-least-thirty-two-characters"
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
    private AuthService authService;

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
    void registerCreatesBidderAccountAndReturnsJwt() throws Exception {
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
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("taylor-bidder"))
            .andExpect(jsonPath("$.user.profileId").value("taylor-bidder"));
    }

    @Test
    void loginReturnsJwtForRegisteredAccount() throws Exception {
        authService.register(new RegisterRequest(
            "casey-auctioneer",
            "casey@example.com",
            "secret123",
            "Casey",
            "AUCTIONEER"
        ));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "casey-auctioneer",
                      "password": "secret123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.user.displayName").value("Casey"))
            .andExpect(jsonPath("$.user.role").value("AUCTIONEER"));
    }

    @Test
    void biddersEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/bidders/all"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void bidderTokenCanReadBiddersEndpoint() throws Exception {
        String token = registerAndGetToken(
            "bidder-user",
            "bidder@example.com",
            "Bidder User",
            "BIDDER"
        );

        mockMvc.perform(get("/bidders/all")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk());
    }

    @Test
    void bidderTokenCannotListAuctionItem() throws Exception {
        String token = registerAndGetToken(
            "bidder-user",
            "bidder@example.com",
            "Bidder User",
            "BIDDER"
        );

        mockMvc.perform(post("/bid/list")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemName": "Camera",
                      "startingPrice": 100,
                      "condition": "USED",
                      "durationAmount": 1,
                      "durationUnit": "DAYS"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void auctioneerTokenCanListAuctionItemWithAuthenticatedIdentity() throws Exception {
        String token = registerAndGetToken(
            "auctioneer-user",
            "auctioneer@example.com",
            "Alex",
            "AUCTIONEER"
        );

        mockMvc.perform(post("/bid/list")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemName": "Laptop",
                      "startingPrice": 300,
                      "condition": "NEW",
                      "durationAmount": 1,
                      "durationUnit": "DAYS"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemId").isString())
            .andExpect(jsonPath("$.itemName").value("Laptop"))
            .andExpect(jsonPath("$.auctioneerId").value("auctioneer-user"))
            .andExpect(jsonPath("$.message").value(containsString("auctioneer-user")));
    }

    @Test
    void bidderTokenCannotSpoofAnotherBidderId() throws Exception {
        String auctioneerToken = registerAndGetToken(
            "auctioneer-user",
            "auctioneer@example.com",
            "Alex",
            "AUCTIONEER"
        );
        String bidderToken = registerAndGetToken(
            "bidder-user",
            "bidder@example.com",
            "Taylor",
            "BIDDER"
        );

        mockMvc.perform(post("/bid/list")
                .header(HttpHeaders.AUTHORIZATION, bearer(auctioneerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemName": "Laptop",
                      "startingPrice": 300,
                      "condition": "USED",
                      "durationAmount": 1,
                      "durationUnit": "DAYS"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/bid/item-1")
                .header(HttpHeaders.AUTHORIZATION, bearer(bidderToken))
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

    @Test
    void authenticatedUserCanReadExpiredBidOutcomes() throws Exception {
        String token = registerAndGetToken(
            "bidder-user",
            "bidder@example.com",
            "Bidder User",
            "BIDDER"
        );

        mockMvc.perform(get("/bid/expired")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk());
    }

    private String registerAndGetToken(String username, String email, String displayName, String role) {
        return authService.register(new RegisterRequest(
            username,
            email,
            "secret123",
            displayName,
            role
        )).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
