package auction_client.models;

import auction_shared.dto.AuctionDTO;
import auction_shared.dto.ItemType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductListManager {
    // ACTIVE auctions: từ RAM (server broadcast UPDATE_BID)
    private List<AuctionDTO> activeAuctions = new ArrayList<>();
    // ENDED/SOLD auctions: từ DB (có pagination)
    private List<AuctionDTO> endedSaledAuctions = new ArrayList<>();
    private int endedTotalCount = 0;

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

    public int getEndedTotalCount() {
        return endedTotalCount;
    }

    public void setEndedTotalCount(int endedTotalCount) {
        this.endedTotalCount = Math.max(0, endedTotalCount);
    }

    public List<AuctionDTO> filterCategory(List<AuctionDTO> auctions, String categoryFilter) {
        return auctions.stream()
                .filter(a -> matchesCategory(a, categoryFilter))
                .sorted(Comparator.comparing(AuctionDTO::getEndTime))
                .collect(Collectors.toList());
    }

    private boolean matchesCategory(AuctionDTO a, String categoryFilter) {
        if ("ALL".equalsIgnoreCase(categoryFilter))
            return true;
        if ("ARTS".equalsIgnoreCase(categoryFilter))
            return a.getItem().getType() == ItemType.ARTS;
        if ("ELECTRONICS".equalsIgnoreCase(categoryFilter))
            return a.getItem().getType() == ItemType.ELECTRONICS;
        if ("VEHICLES".equalsIgnoreCase(categoryFilter))
            return a.getItem().getType() == ItemType.VEHICLES;
        return true;
    }

    public List<AuctionDTO> paginate(List<AuctionDTO> list, int page, int pageSize) {
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        int from = page * pageSize;
        if (from >= list.size())
            return Collections.emptyList();
        int to = Math.min(from + pageSize, list.size());
        return list.subList(from, to);
    }
}
