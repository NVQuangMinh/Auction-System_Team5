package auction_client.exception;

public class AuctionClosedException extends RuntimeException {

    public AuctionClosedException(String message) {
        super(message);
    }

    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}