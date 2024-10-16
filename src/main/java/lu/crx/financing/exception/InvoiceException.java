package lu.crx.financing.exception;

/**
 * Invoice exception is an exception an invoice can throw only for business reasons.
 */
public class InvoiceException extends RuntimeException {
    public InvoiceException(String message) {
        super(message);
    }
}
