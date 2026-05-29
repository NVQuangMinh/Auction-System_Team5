package auctionserver.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import auctionserver.exception.BidException;
import auctionserver.service.ValidatorService;
import auctionshared.dto.AuctionStatus;

public class Auction implements Serializable {
    // CONSTANTS (cho anti-snipe)
    private static final long SNIPING_GRACE_SECONDS = 30;
    private static final long EXTENSION_SECONDS = 30;

    // AUCTION INFO
    private String auctionId;
    private Item item;
    private double startingPrice;
    private double buyOutPrice;
    private double tickSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean antiSniping;
    private double currentHighestBid;
    private AuctionStatus status;

    // BID HISTORY
    private String winnerId;
    private final List<BidTransaction> bidHistory = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();
    // Transient: chỉ dùng tạm trong RAM để hỗ trợ revertBuyOut, không cần serialize
    private transient User originalOwnerBeforeBuyOut;

    public Auction(Item item, double startingPrice, double buyOutPrice, double tickSize, LocalDateTime startTime,
            LocalDateTime endTime, boolean antiSniping) {
        this.status = AuctionStatus.ACTIVE;
        this.auctionId = item.getId(); // set từ item
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.startTime = startTime;
        this.endTime = endTime;
        this.antiSniping = antiSniping;
        this.winnerId = null;
    }

    /**
     * Constructor dùng để rebuild Auction entity từ database khi server khởi động
     * lại.
     * currentHighestBid và status được truyền trực tiếp thay vì tính toán lại.
     */
    public Auction(Item item, double startingPrice, double buyOutPrice, double tickSize,
            LocalDateTime startTime, LocalDateTime endTime, boolean antiSniping,
            double currentHighestBid, AuctionStatus status) {
        this.auctionId = item.getId();
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.startTime = startTime;
        this.endTime = endTime;
        this.antiSniping = antiSniping;
        this.currentHighestBid = currentHighestBid;
        this.status = status;
        this.winnerId = null;
    }

    public void setBidHistory(List<BidTransaction> history) {
        this.bidHistory.clear();
        this.bidHistory.addAll(history);
    }

    public void addTransaction(BidTransaction transaction) {
        bidHistory.add(transaction);
    }

    public double getCurrentHighestBid() {
        if (bidHistory.isEmpty()) {
            return startingPrice;
        } else {
            return bidHistory.get(bidHistory.size() - 1).getBidAmount();
        }
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public void endAuction() {
        lock.lock();
        try {
            if (status != AuctionStatus.ACTIVE) {
                return;
            }
            status = AuctionStatus.ENDED;
            if (!bidHistory.isEmpty()) {
                for (int i = bidHistory.size() - 1; i > -1; i--) {
                    if (bidHistory.get(i).getBidder() != null) {
                        winnerId = bidHistory.get(i).getBidder().getId();
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void extendTime() {
        lock.lock();
        try {
            if (status != AuctionStatus.ACTIVE) {
                return;
            }

            long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
            if (remaining <= SNIPING_GRACE_SECONDS && remaining > 0) {
                endTime = endTime.plusSeconds(EXTENSION_SECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    public void placeBid(BidTransaction transaction) throws BidException {
        lock.lock();
        try {
            ValidatorService.validateBid(this, transaction);
            addTransaction(transaction);
            setCurrentHighestBid(transaction.getBidAmount());
        } finally {
            lock.unlock();
        }
    }

    public void buyOut(BidTransaction transaction) throws BidException {
        lock.lock();
        try {
            ValidatorService.validateBuyOut(this, transaction);
            this.originalOwnerBeforeBuyOut = item.getOwner();
            item.setOwner(transaction.getBidder());
            status = AuctionStatus.SOLD;
            winnerId = transaction.getBidder().getId();
            addTransaction(transaction);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Bid lỗi: DB transaction thất bại
     * NHƯNG: in-memory state đã được cập nhật
     * => bidHistory và currentHighestBid trên RAM đồng bộ lại DB
     * /Cách hoạt động:
     * 1. auction.placeBid(transaction) → thêm transaction vào bidHistory
     * → set currentHighestBid = transaction.getBidAmount()
     * 2. bidDAO.insert(transaction, conn) → insert vào DB
     * 3. auctionDAO.updateHighestBid(...) → update current_highest_bid trong DB
     * 4. conn.commit() → ❌ THẤT BẠI (VD: deadlock, constraint violation, timeout,
     * blah bleh)
     * → conn.rollback() → DB rollback, nhưng RAM vẫn giữ bid mới!
     * → auction.revertLastBid(transaction) → hoàn tác RAM
     */

    public void revertLastBid(BidTransaction transaction) {
        lock.lock();
        try {
            if (!bidHistory.isEmpty() && bidHistory.get(bidHistory.size() - 1).equals(transaction)) {
                bidHistory.remove(bidHistory.size() - 1); // Xóa giao dịch lỗi

                // Cập nhật lại giá cao nhất về giao dịch liền kề trước đó, hoặc giá khởi điểm
                if (bidHistory.isEmpty()) {
                    this.currentHighestBid = this.startingPrice;
                } else {
                    this.currentHighestBid = bidHistory.get(bidHistory.size() - 1).getBidAmount();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Hoàn tác Buy Out khi DB Transaction bị lỗi.
     * Trả owner về chủ cũ, đặt lại status = ACTIVE, xóa winnerId.
     */
    public void revertBuyOut(BidTransaction transaction) {
        lock.lock();
        try {
            if (originalOwnerBeforeBuyOut != null) {
                item.setOwner(originalOwnerBeforeBuyOut);
                originalOwnerBeforeBuyOut = null;
            }
            status = AuctionStatus.ACTIVE;
            winnerId = null;
        } finally {
            lock.unlock();
        }
    }

    public Item getItem() {
        return item;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getBuyOutPrice() {
        return buyOutPrice;
    }

    public double getTickSize() {
        return tickSize;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public String getAuctionId() {
        return auctionId;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public boolean isAntiSniping() {
        return antiSniping;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Chuẩn bị bid trong RAM (validate + cập nhật state).
     * KHÔNG lock — caller phải acquire lock trước và giữ qua DB transaction.
     * Dùng bởi BidService để giữ lock qua toàn bộ operation (validation + DB
     * persist).
     */
    public void prepareBidInMemory(BidTransaction transaction) throws BidException {
        ValidatorService.validateBid(this, transaction);
        addTransaction(transaction);
        setCurrentHighestBid(transaction.getBidAmount());
    }
}
