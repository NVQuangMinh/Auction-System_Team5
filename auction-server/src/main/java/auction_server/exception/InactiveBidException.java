package auction_server.exception;

public class InactiveBidException extends BidException {
    public InactiveBidException(String message) {
        super(message);
    }
}
