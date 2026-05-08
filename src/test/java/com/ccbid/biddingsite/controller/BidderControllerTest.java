package com.ccbid.biddingsite.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.service.BidderService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BidderControllerTest {

    @Mock
    private BidderService bidderService;

    @InjectMocks
    private BidderController controller;

    @Test
    void bidderLookupBindsPathVariableCorrectly() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(bidderService.getBidder("bidder-7")).thenReturn(new Bidder("bidder-7", "Taylor"));

        mockMvc.perform(get("/bidders/bidder-7"))
            .andExpect(status().isOk());

        verify(bidderService).getBidder("bidder-7");
    }
}
