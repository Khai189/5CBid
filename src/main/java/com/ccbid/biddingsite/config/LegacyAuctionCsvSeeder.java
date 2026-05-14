package com.ccbid.biddingsite.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.Bid;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.repository.AuctioneerRepo;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.BidderRepo;
import com.ccbid.biddingsite.repository.ItemRepo;

@Component
public class LegacyAuctionCsvSeeder implements ApplicationRunner {

    private static final String LEGACY_AUCTIONEER_ID = "legacy-import";
    private static final String LEGACY_AUCTIONEER_NAME = "Legacy Auction Import";

    private final Resource auctionCsv;
    private final boolean enabled;
    private final AuctioneerRepo auctioneerRepo;
    private final ItemRepo itemRepo;
    private final BidderRepo bidderRepo;
    private final BidRepo bidRepo;

    public LegacyAuctionCsvSeeder(
        @Value("classpath:auction.csv") Resource auctionCsv,
        @Value("${app.seed.legacy-auctions.enabled:false}") boolean enabled,
        AuctioneerRepo auctioneerRepo,
        ItemRepo itemRepo,
        BidderRepo bidderRepo,
        BidRepo bidRepo
    ) {
        this.auctionCsv = auctionCsv;
        this.enabled = enabled;
        this.auctioneerRepo = auctioneerRepo;
        this.itemRepo = itemRepo;
        this.bidderRepo = bidderRepo;
        this.bidRepo = bidRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }

        Auctioneer legacyAuctioneer = auctioneerRepo.findById(LEGACY_AUCTIONEER_ID)
            .orElseGet(() -> auctioneerRepo.save(new Auctioneer(LEGACY_AUCTIONEER_ID, LEGACY_AUCTIONEER_NAME)));
        Map<String, LegacyAuctionRecord> auctions = loadAuctions();

        int auctionIndex = 0;
        for (LegacyAuctionRecord auction : auctions.values()) {
            String legacyItemId = toLegacyItemId(auction.auctionId());
            BidItem item = itemRepo.findById(legacyItemId).orElseGet(() -> {
                BidItem seeded = new BidItem();
                seeded.setItemId(legacyItemId);
                seeded.setArchived(true);
                return seeded;
            });
            item.setItemName(auction.itemName());
            item.setStartingPrice(roundCurrency(auction.openBid()));
            item.setDescription(buildDescription(auction));
            item.setArchived(true);
            item.setAuctioneer(legacyAuctioneer);
            itemRepo.save(item);

            if (!bidRepo.findAllByItemIdOrderByCreatedAtAsc(legacyItemId).isEmpty()) {
                auctionIndex++;
                continue;
            }

            Instant auctionStart = Instant.parse("2019-01-01T00:00:00Z").plus(auctionIndex, ChronoUnit.DAYS);
            int bidIndex = 0;
            for (LegacyBidRecord legacyBid : auction.bids()) {
                bidderRepo.findById(legacyBid.bidderId())
                    .orElseGet(() -> bidderRepo.save(new Bidder(legacyBid.bidderId(), legacyBid.bidderId())));

                Bid bid = new Bid();
                bid.setItemId(item.getItemId());
                bid.setBidderId(legacyBid.bidderId());
                bid.setAmount(roundCurrency(legacyBid.bidAmount()));
                bid.setCreatedAt(toCreatedAt(auctionStart, legacyBid.bidTimeDays(), bidIndex));
                bid.setActive(false);
                bidRepo.save(bid);
                bidIndex++;
            }
            auctionIndex++;
        }
    }

    private Map<String, LegacyAuctionRecord> loadAuctions() throws IOException {
        Map<String, LegacyAuctionRecord> auctions = new LinkedHashMap<>();

        try (InputStream inputStream = auctionCsv.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null) {
                return auctions;
            }

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                List<String> cells = parseCsvLine(line);
                if (cells.size() < 9) {
                    continue;
                }

                String auctionId = cells.get(0);
                double bid = parseDouble(cells.get(1));
                double bidTime = parseDouble(cells.get(2));
                String bidder = cells.get(3);
                double openBid = parseDouble(cells.get(5));
                double finalPrice = parseDouble(cells.get(6));
                String itemName = cells.get(7);
                String auctionType = cells.get(8);

                LegacyAuctionRecord auction = auctions.computeIfAbsent(
                    auctionId,
                    ignored -> new LegacyAuctionRecord(auctionId, itemName, openBid, finalPrice, auctionType, new ArrayList<>())
                );
                auction.bids().add(new LegacyBidRecord(bid, bidTime, bidder));
            }
        }

        return auctions;
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        cells.add(current.toString());
        return cells;
    }

    private String toLegacyItemId(String auctionId) {
        return "legacy-" + auctionId;
    }

    private String buildDescription(LegacyAuctionRecord auction) {
        return "Imported legacy auction data. Type: " + auction.auctionType()
            + ". Historical final price: " + roundCurrency(auction.finalPrice())
            + ". Seeded as archived listing with inactive historical bids.";
    }

    private Instant toCreatedAt(Instant auctionStart, double bidTimeDays, int bidIndex) {
        long offsetSeconds = Math.round(bidTimeDays * 24 * 60 * 60);
        return auctionStart.plusSeconds(offsetSeconds).plusMillis(bidIndex);
    }

    private int roundCurrency(double amount) {
        return (int) Math.round(amount);
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private record LegacyAuctionRecord(
        String auctionId,
        String itemName,
        double openBid,
        double finalPrice,
        String auctionType,
        List<LegacyBidRecord> bids
    ) {}

    private record LegacyBidRecord(
        double bidAmount,
        double bidTimeDays,
        String bidderId
    ) {}
}
