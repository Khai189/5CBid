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

import com.ccbid.biddingsite.dataStructures.ItemBid;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepo itemRepo;
    @Mock
    private BidService bidService;

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

        List<String> filtered = itemService.getItems(null, null, null, 10.0, null).stream()
            .map(summary -> summary.itemId())
            .toList();

        assertEquals(List.of("item-1"), filtered);
    }

    @Test
    void getItemsIncludesLiveBidSnapshot() {
        BidItem item = new BidItem();
        item.setItemId("item-1");
        item.setItemName("Desk Lamp");
        item.setStartingPrice(10.0);
        item.setArchived(false);
        Auctioneer auctioneer = new Auctioneer();
        auctioneer.setAuctioneerId("seller-1");
        auctioneer.setName("Sam");
        item.setAuctioneer(auctioneer);

        ItemBid itemBid = new ItemBid(auctioneer, item);
        itemBid.addBid("bidder-1", 18);
        itemBid.addBid("bidder-2", 22);

        when(itemRepo.findAllByArchivedFalseOrderByItemIdAsc()).thenReturn(List.of(item));
        when(bidService.getLiveItemBidOrNull("item-1")).thenReturn(itemBid);

        var summary = itemService.getItems(null, null, null, null, null).getFirst();

        assertEquals("bidder-2", summary.highestBidderId());
        assertEquals(22, summary.highestBidAmount());
        assertEquals(2, summary.bidCount());
    }
}
