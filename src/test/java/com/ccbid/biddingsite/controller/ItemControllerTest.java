package com.ccbid.biddingsite.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ccbid.biddingsite.dto.ItemListingSummaryResponse;
import com.ccbid.biddingsite.service.ItemService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController controller;

    @Test
    void getItemsReturnsOk() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(itemService.getItems(null, null, null, null, null))
            .thenReturn(List.of(new ItemListingSummaryResponse(null, null, null, null, null, null, null, null, null, 0, null)));

        mockMvc.perform(get("/items/all"))
            .andExpect(status().isOk());

        verify(itemService).getItems(null, null, null, null, null);
    }

    @Test
    void getItemsPassesSearchFilters() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(itemService.getItems("bike", "seller-1", 25.0, 200.0, "USED"))
            .thenReturn(List.of(new ItemListingSummaryResponse(null, null, null, null, null, null, null, null, null, 0, null)));

        mockMvc.perform(get("/items/all")
                .param("query", "bike")
                .param("auctioneerId", "seller-1")
                .param("minPrice", "25")
                .param("maxPrice", "200")
                .param("condition", "USED"))
            .andExpect(status().isOk());

        verify(itemService).getItems("bike", "seller-1", 25.0, 200.0, "USED");
    }

    @Test
    void getItemBindsPathVariableCorrectly() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        ItemListingSummaryResponse item = new ItemListingSummaryResponse(
            "item-42", "Lamp", 10.0, null, "USED", "seller-1", "Sam", "bidder-9", 18, 3, null
        );
        when(itemService.getItem("item-42")).thenReturn(item);

        mockMvc.perform(get("/items/item-42"))
            .andExpect(status().isOk());

        verify(itemService).getItem("item-42");
    }
}
