package auction_server.exception;

public class TransactionFailedException extends DatabaseException {
    public TransactionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
