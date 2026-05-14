package com.ccbid.biddingsite.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepo itemRepo;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItemsRejectsMaxPriceBelowFiftyCents() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> itemService.getItems(null, null, null, 0.25, null));

        assertEquals("maxPrice must be at least 0.5", ex.getMessage());
    }

    @Test
    void getItemsFiltersByMaximumPrice() {
        BidItem cheap = new BidItem();
        cheap.setItemId("item-1");
        cheap.setStartingPrice(3.0);

        BidItem expensive = new BidItem();
        expensive.setItemId("item-2");
        expensive.setStartingPrice(20.0);

        when(itemRepo.findAllByArchivedFalseOrderByItemIdAsc()).thenReturn(List.of(cheap, expensive));

        List<BidItem> filtered = itemService.getItems(null, null, null, 10.0, null);

        assertEquals(List.of("item-1"), filtered.stream().map(BidItem::getItemId).toList());
    }
}
