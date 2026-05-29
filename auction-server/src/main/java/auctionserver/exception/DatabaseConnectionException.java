package auctionserver.exception;

public class DatabaseConnectionException extends DatabaseException {
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
