package lu.crx.financing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * InvoiceStatus enumeration contains all the possible statuses an invoice can have.
 */
@AllArgsConstructor
@Getter
public enum InvoiceStatus {
    PENDING(1, "PENDING"),
    CANCELED(2, "CANCELED"),
    FINANCED(3, "FINANCED"),
    NON_FINANCED(4, "NON_FINANCED"),
    SUSPENDED(5, "SUSPENDED");

    private final int id;
    private final String description;

}
