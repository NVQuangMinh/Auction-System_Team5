package auctionclient.models;

import auctionshared.dto.AuctionDTO;
import auctionshared.dto.ItemType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductListManager {
    // ACTIVE auctions: từ RAM (server broadcast UPDATE_BID)
    private List<AuctionDTO> activeAuctions = new ArrayList<>();
    // ENDED/SOLD auctions: từ DB
    private List<AuctionDTO> endedSaledAuctions = new ArrayList<>();

    public List<AuctionDTO> getActiveAuctions() {
        return activeAuctions;
    }

    public void setActiveAuctions(List<AuctionDTO> activeAuctions) {
        this.activeAuctions = activeAuctions != null ? activeAuctions : new ArrayList<>();
    }

    public List<AuctionDTO> getEndedSaledAuctions() {
        return endedSaledAuctions;
    }

    public void setEndedSaledAuctions(List<AuctionDTO> endedSaledAuctions) {
        this.endedSaledAuctions = endedSaledAuctions != null ? endedSaledAuctions : new ArrayList<>();
    }

    public List<AuctionDTO> filterCategory(List<AuctionDTO> auctions, String categoryFilter) {
        return auctions.stream()
                .filter(a -> matchesCategory(a, categoryFilter))
                .sorted(Comparator.comparing(AuctionDTO::getEndTime))
                .collect(Collectors.toList());
    }

    private boolean matchesCategory(AuctionDTO a, String categoryFilter) {
        if ("ALL".equalsIgnoreCase(categoryFilter)) {
            return true;
        }
        if ("ARTS".equalsIgnoreCase(categoryFilter)) {
            return a.getItem().getType() == ItemType.ARTS;
        }
        if ("ELECTRONICS".equalsIgnoreCase(categoryFilter)) {
            return a.getItem().getType() == ItemType.ELECTRONICS;
        }
        if ("VEHICLES".equalsIgnoreCase(categoryFilter)) {
            return a.getItem().getType() == ItemType.VEHICLES;
        }
        return true;
    }

}
