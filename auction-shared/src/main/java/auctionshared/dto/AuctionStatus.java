package auctionshared.dto;

public enum AuctionStatus {
    ACTIVE,
    ENDED,
    SOLD,   // dành cho buy out
    BANNED  // bị admin xóa, giữ lại lịch sử
}
