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
        when(itemService.getItems()).thenReturn(List.of(new BidItem()));

        mockMvc.perform(get("/items/all"))
            .andExpect(status().isOk());

        verify(itemService).getItems();
    }

    @Test
    void getItemBindsPathVariableCorrectly() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        BidItem item = new BidItem();
        item.setItemId("item-42");
        when(itemService.getItem("item-42")).thenReturn(item);

        mockMvc.perform(get("/items/item-42"))
            .andExpect(status().isOk());

        verify(itemService).getItem("item-42");
    }
}
