package lu.crx.financing.entities;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A financing result entity contains all the information needed for a invoice with status FINANCED issued
 * with {@link Purchaser} who will finally finance the invoice.
 */
@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancingResult implements Serializable {

    @Id
    private long invoiceId;

    @Basic(optional = false)
    private long initialAmount;

    @Basic(optional = false)
    private long earlyPaymentAmount;

    @Basic(optional = false)
    private long financingRate;

    @Basic(optional = false)
    private LocalDate financingDate;

    @Basic(optional = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    private Purchaser purchaser;

}
