package com.ccbid.biddingsite.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.service.RecService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecControllerTest {

    @Mock
    private RecService recService;

    @InjectMocks
    private RecController controller;

    @Test
    void recEndpointBindsPathVariablesCorrectly() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(recService.getRecommendations("bidder-1", "item-1", 3)).thenReturn(List.of(new BidItem()));

        mockMvc.perform(get("/rec/bidder-1/item-1/3"))
            .andExpect(status().isOk());

        verify(recService).getRecommendations("bidder-1", "item-1", 3);
    }

    @Test
    void feedEndpointBindsPathVariableCorrectly() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(recService.getFeed("bidder-2")).thenReturn(List.of());

        mockMvc.perform(get("/feed/bidder-2"))
            .andExpect(status().isOk());

        verify(recService).getFeed("bidder-2");
    }
}
