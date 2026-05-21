package auction_server.exception;

public class SelfBiddingException extends BidException{
    public SelfBiddingException(String message) {
        super(message);
    }
}
