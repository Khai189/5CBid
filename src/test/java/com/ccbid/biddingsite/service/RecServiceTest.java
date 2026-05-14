package com.ccbid.biddingsite.service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccbid.biddingsite.models.Bid;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.ItemRepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecServiceTest {

    @Mock
    private ItemRepo itemRepo;

    @Mock
    private BidRepo bidRepo;

    @InjectMocks
    private RecService recService;

    @Test
    void getRecommendationsReturnsClosestConnectedItems() {
        BidItem item1 = item("item-1", "Camera");
        BidItem item2 = item("item-2", "Lens");
        BidItem item3 = item("item-3", "Tripod");

        when(itemRepo.findAllByArchivedFalseOrderByItemIdAsc()).thenReturn(List.of(item1, item2, item3));
        when(bidRepo.findAllByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(
            bid("item-1", "bidder-a"),
            bid("item-2", "bidder-b"),
            bid("item-1", "bidder-b"),
            bid("item-2", "bidder-c"),
            bid("item-3", "bidder-c")
        ));
        when(bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(bid("item-1", "bidder-a")));

        List<BidItem> recommendations = recService.getRecommendations("bidder-a", "item-1", 5);

        assertEquals(List.of("item-2", "item-3"),
            recommendations.stream().map(BidItem::getItemId).toList());
    }

    @Test
    void getFeedExcludesItemsAlreadyBidOn() {
        BidItem item1 = item("item-1", "Camera");
        BidItem item2 = item("item-2", "Lens");
        BidItem item3 = item("item-3", "Tripod");

        when(itemRepo.findAllByArchivedFalseOrderByItemIdAsc()).thenReturn(List.of(item1, item2, item3));
        when(bidRepo.findAllByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(
            bid("item-1", "bidder-a"),
            bid("item-1", "bidder-b"),
            bid("item-2", "bidder-b"),
            bid("item-2", "bidder-c"),
            bid("item-3", "bidder-c")
        ));
        when(bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(bid("item-1", "bidder-a")));

        List<BidItem> feed = recService.getFeed("bidder-a");

        assertEquals(List.of("item-2", "item-3"),
            feed.stream().map(BidItem::getItemId).toList());
    }

    private BidItem item(String itemId, String itemName) {
        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName(itemName);
        return item;
    }

    private Bid bid(String itemId, String bidderId) {
        Bid bid = new Bid();
        bid.setItemId(itemId);
        bid.setBidderId(bidderId);
        bid.setAmount(100);
        bid.setCreatedAt(Instant.now());
        bid.setActive(true);
        return bid;
    }
}
