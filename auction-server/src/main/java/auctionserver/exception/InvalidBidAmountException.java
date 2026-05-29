package auctionserver.exception;

public class InvalidBidAmountException extends BidException {
    public InvalidBidAmountException(String message) {
        super(message);
    }
}
