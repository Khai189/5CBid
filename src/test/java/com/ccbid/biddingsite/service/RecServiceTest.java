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

    @Mock
    private BidService bidService;

    @InjectMocks
    private RecService recService;

    @Test
    void getRecommendationsReturnsClosestConnectedItems() {
        BidItem item1 = item("item-1", "Camera", 8);
        BidItem item2 = item("item-2", "Lens", 12);
        BidItem item3 = item("item-3", "Tripod", 15);

        when(itemRepo.findAllByArchivedFalseOrderByItemIdAsc()).thenReturn(List.of(item1, item2, item3));
        when(bidRepo.findAllByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(
            bid("item-1", "bidder-a"),
            bid("item-2", "bidder-b"),
            bid("item-1", "bidder-b"),
            bid("item-2", "bidder-c"),
            bid("item-3", "bidder-c")
        ));
        when(bidRepo.findAllByBidderIdOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(bid("item-1", "bidder-a", 20)));
        when(bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(bid("item-1", "bidder-a")));

        List<BidItem> recommendations = recService.getRecommendations("bidder-a", "item-1", 5);

        assertEquals(List.of("item-2", "item-3"),
            recommendations.stream().map(BidItem::getItemId).toList());
    }

    @Test
    void getFeedExcludesItemsAlreadyBidOn() {
        BidItem item1 = item("item-1", "Camera", 8);
        BidItem item2 = item("item-2", "Lens", 12);
        BidItem item3 = item("item-3", "Tripod", 15);

        when(itemRepo.findAllByArchivedFalseOrderByItemIdAsc()).thenReturn(List.of(item1, item2, item3));
        when(bidRepo.findAllByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(
            bid("item-1", "bidder-a"),
            bid("item-1", "bidder-b"),
            bid("item-2", "bidder-b"),
            bid("item-2", "bidder-c"),
            bid("item-3", "bidder-c")
        ));
        when(bidRepo.findAllByBidderIdOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(bid("item-1", "bidder-a", 20)));
        when(bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(bid("item-1", "bidder-a")));

        List<BidItem> feed = recService.getFeed("bidder-a");

        assertEquals(List.of("item-2", "item-3"),
            feed.stream().map(BidItem::getItemId).toList());
    }

    @Test
    void getRecommendationsPrefersItemsWithinBidderPriceRange() {
        BidItem item1 = item("item-1", "Desk Lamp", 8);
        BidItem item2 = item("item-2", "Mini Fridge", 25);
        BidItem item3 = item("item-3", "Notebook Set", 9);

        when(itemRepo.findAllByArchivedFalseOrderByItemIdAsc()).thenReturn(List.of(item1, item2, item3));
        when(bidRepo.findAllByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(
            bid("item-1", "bidder-a", 8),
            bid("item-1", "bidder-b", 8),
            bid("item-2", "bidder-b", 25),
            bid("item-1", "bidder-c", 8),
            bid("item-3", "bidder-c", 9)
        ));
        when(bidRepo.findAllByBidderIdOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(
                bid("item-1", "bidder-a", 7),
                bid("item-1", "bidder-a", 9)
            ));
        when(bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc("bidder-a"))
            .thenReturn(List.of(bid("item-1", "bidder-a", 8)));

        List<BidItem> recommendations = recService.getRecommendations("bidder-a", "item-1", 5);

        assertEquals(List.of("item-3", "item-2"),
            recommendations.stream().map(BidItem::getItemId).toList());
    }

    private BidItem item(String itemId, String itemName, double startingPrice) {
        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        return item;
    }

    private Bid bid(String itemId, String bidderId) {
        return bid(itemId, bidderId, 100);
    }

    private Bid bid(String itemId, String bidderId, int amount) {
        Bid bid = new Bid();
        bid.setItemId(itemId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        bid.setCreatedAt(Instant.now());
        bid.setActive(true);
        return bid;
    }
}
