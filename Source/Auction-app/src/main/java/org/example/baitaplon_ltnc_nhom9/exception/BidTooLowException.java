package exception;

public class BidTooLowException extends Exception {
    public BidTooLowException(String message) {
        super(message);
    }
}