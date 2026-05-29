package auctionserver.exception;

public class UserBannedException extends RuntimeException {
    public UserBannedException(String message) {
        super(message);
    }
}
