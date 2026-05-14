package com.ccbid.biddingsite.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccbid.biddingsite.dataStructures.ItemBid;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.Bid;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.models.ItemCondition;
import com.ccbid.biddingsite.repository.AuctioneerRepo;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.BidderRepo;
import com.ccbid.biddingsite.repository.ItemRepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private ItemRepo itemRepo;

    @Mock
    private AuctioneerRepo auctioneerRepo;

    @Mock
    private BidRepo bidRepo;

    @Mock
    private BidderRepo bidderRepo;

    @InjectMocks
    private BidService service;

    @BeforeEach
    void setUp() {
        when(auctioneerRepo.findById("auctioneer-1")).thenReturn(Optional.empty());
        when(auctioneerRepo.save(any(Auctioneer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepo.save(any(BidItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void placeBidRejectsAmountBelowStartingPrice() {
        registerItem("item-1", 100);
        when(bidderRepo.findById("bidder-1")).thenReturn(Optional.of(new Bidder("bidder-1", "Sam")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.placeBid("item-1", "bidder-1", 90));

        assertEquals("Bid 90 is below starting price 100.0", ex.getMessage());
    }

    @Test
    void placeBidPersistsAndTracksHighestBid() {
        registerItem("item-2", 100);
        when(bidderRepo.findById("bidder-1")).thenReturn(Optional.of(new Bidder("bidder-1", "Sam")));
        when(bidRepo.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String response = service.placeBid("item-2", "bidder-1", 150);

        assertEquals(150, service.getHighestBid("item-2"));
        assertEquals("bidder-1", service.getHighestBidder("item-2"));
        assertEquals(
            "Bid placed: bidder=bidder-1 amount=150 item=item-2 | currentHighest=150 by bidder-1",
            response
        );
        verify(bidRepo, times(1)).save(any(Bid.class));
    }

    @Test
    void activeBidSummariesForBidderIncludeHighestBidAndViewerBid() {
        registerItem("item-4", 50);
        when(bidderRepo.findById("bidder-1")).thenReturn(Optional.of(new Bidder("bidder-1", "Sam")));
        when(bidderRepo.findById("bidder-2")).thenReturn(Optional.of(new Bidder("bidder-2", "Alex")));
        when(bidRepo.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc("bidder-1"))
            .thenReturn(List.of(new Bid(1L, "item-4", "bidder-1", 60, null, true)));

        service.placeBid("item-4", "bidder-1", 60);
        service.placeBid("item-4", "bidder-2", 80);

        var summaries = service.getActiveBidSummariesForBidder("bidder-1");

        assertEquals(1, summaries.size());
        assertEquals("item-4", summaries.getFirst().itemId());
        assertEquals("bidder-2", summaries.getFirst().highestBidderId());
        assertEquals(80, summaries.getFirst().highestBidAmount());
        assertEquals(60, summaries.getFirst().viewerBidAmount());
    }

    @Test
    void activeBidSummariesForAuctioneerIncludeHighestBidderAndAmount() {
        BidItem item = registerItem("item-5", 75);
        item.setDescription("Dorm fridge");
        when(itemRepo.findAllByAuctioneer_AuctioneerIdOrderByItemIdAsc("auctioneer-1")).thenReturn(List.of(item));
        when(bidderRepo.findById("bidder-1")).thenReturn(Optional.of(new Bidder("bidder-1", "Sam")));
        when(bidRepo.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.placeBid("item-5", "bidder-1", 95);

        var summaries = service.getActiveBidSummariesForAuctioneer("auctioneer-1");

        assertEquals(1, summaries.size());
        assertEquals("item-5", summaries.getFirst().itemId());
        assertEquals("Dorm fridge", summaries.getFirst().description());
        assertEquals("bidder-1", summaries.getFirst().highestBidderId());
        assertEquals(95, summaries.getFirst().highestBidAmount());
        assertNull(summaries.getFirst().viewerBidAmount());
    }

    @Test
    void removeBidMarksActiveBidsInactive() {
        registerItem("item-3", 100);
        service.getItem("item-3").addBid("bidder-1", 175);

        Bid firstBid = new Bid(1L, "item-3", "bidder-1", 120, null, true);
        Bid highestBid = new Bid(2L, "item-3", "bidder-1", 175, null, true);
        List<Bid> activeBids = List.of(firstBid, highestBid);
        when(bidRepo.findAllByItemIdAndBidderIdAndActiveTrue("item-3", "bidder-1")).thenReturn(activeBids);

        String response = service.removeBid("item-3", "bidder-1");

        assertEquals("Removed bid by bidder-1 on item-3", response);
        assertEquals(-1, service.getHighestBid("item-3"));
        assertFalse(firstBid.isActive());
        assertFalse(highestBid.isActive());
        verify(bidRepo).saveAll(eq(activeBids));
    }

    @Test
    void addItemGeneratesListingIdAutomatically() {
        when(itemRepo.existsById(any(String.class))).thenReturn(false);

        ItemBid created = service.addItem(
            "Desk Lamp",
            12.0,
            "Warm desk light",
            ItemCondition.USED,
            "auctioneer-1",
            "Alex"
        );

        assertNotNull(created.getItem().getItemId());
        assertFalse(created.getItem().getItemId().isBlank());
        assertEquals("Desk Lamp", created.getItem().getItemName());
    }

    private BidItem registerItem(String itemId, int startingPrice) {
        when(itemRepo.existsById(itemId)).thenReturn(false);
        when(itemRepo.findByItemId(itemId)).thenReturn(Optional.of(item(itemId, "Locked Item")));

        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName("Test Item");
        item.setStartingPrice((double) startingPrice);
        item.setCondition(ItemCondition.NEW);
        item.setArchived(false);

        Auctioneer auctioneer = new Auctioneer();
        auctioneer.setAuctioneerId("auctioneer-1");
        auctioneer.setName("Alex");

        service.addItem(item, auctioneer);
        return item;
    }

    private BidItem item(String itemId, String itemName) {
        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName(itemName);
        item.setCondition(ItemCondition.NEW);
        item.setArchived(false);
        return item;
    }
}
