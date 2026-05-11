package com.ccbid.biddingsite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:security-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
class SecurityIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void biddersEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/bidders/all"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void bidderCanReadBiddersEndpoint() throws Exception {
        mockMvc.perform(get("/bidders/all")
                .with(httpBasic("bidder", "bidder")))
            .andExpect(status().isOk());
    }

    @Test
    void bidderCannotListAuctionItem() throws Exception {
        mockMvc.perform(post("/bid/list")
                .with(httpBasic("bidder", "bidder"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemId": "forbidden-item",
                      "itemName": "Camera",
                      "startingPrice": 100,
                      "auctioneerId": "auctioneer-1",
                      "auctioneerName": "Alex"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void auctioneerCanListAuctionItem() throws Exception {
        mockMvc.perform(post("/bid/list")
                .with(httpBasic("auctioneer", "auctioneer"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemId": "listed-by-auctioneer",
                      "itemName": "Laptop",
                      "startingPrice": 300,
                      "auctioneerId": "auctioneer-1",
                      "auctioneerName": "Alex"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Listed listed-by-auctioneer")));
    }
}
