package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.FinancingResult;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.enums.InvoiceStatus;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
import lu.crx.financing.repositories.FinancingResultRepository;
import lu.crx.financing.repositories.InvoiceRepository;
import lu.crx.financing.repositories.PurchaserRepository;
import lu.crx.financing.utils.EligibilityCheckUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class FinancingService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaserRepository purchaserRepository;
    private final FinancingResultRepository financingResultRepository;
    private final EligibilityCheckUtils eligibilityCheckUtils;

    @Transactional
    public void finance() {
        long start = System.currentTimeMillis();
        log.info("Financing started");
        // Set batch size
        int batchSize = 10;
        int page = 0;

        // Batch process invoices using pagination
        while (true) {
            Pageable pageable = PageRequest.of(page, batchSize);
            List<Invoice> invoices = invoiceRepository.findAllByInvoiceStatus(InvoiceStatus.PENDING.getDescription(), pageable).getContent();

            if (invoices.isEmpty()) {
                log.info("No more pending invoices to process");
                break;
            }
            invoices.forEach(this::processInvoice);

        }
        long end = System.currentTimeMillis();
        long diff = end - start;
        log.info("Financing completed in {} millis", diff);

    }


    private void processInvoice(Invoice invoice) {
        try {
            LocalDate currentDate = LocalDate.now();

            if (invoice.getMaturityDate().isAfter(currentDate)) {
                log.info("Processing invoice: {} with maturity date: {}", invoice.getId(), invoice.getMaturityDate());

                // Calculate financing term in days
                int financingTermInDays = calculateFinancingTerm(invoice.getMaturityDate());

                // Get the creditor
                Creditor creditor = invoice.getCreditor();

                List<Purchaser> purchasers = purchaserRepository.findPurchasersByCreditor(creditor);

                // Get eligible purchasers
                List<Purchaser> eligiblePurchasers = eligibilityCheckUtils.getEligiblePurchasers(purchasers, creditor, financingTermInDays);
                log.info("Eligible purchasers found: {} for invoice: {}", eligiblePurchasers.size(), invoice.getId());

                if (!eligiblePurchasers.isEmpty()) {
                    // Select the purchaser with the best financing rate
                    Pair<Purchaser, Integer> selectedPurchaserAndRate = selectPurchaser(eligiblePurchasers, creditor, financingTermInDays);
                    log.info("Selected purchaser: {} with financing rate: {} for invoice: {}", selectedPurchaserAndRate.getFirst(), selectedPurchaserAndRate.getSecond(), invoice.getId());

                    // Perform financing and update the invoice status
                    performFinancing(invoice, selectedPurchaserAndRate);
                } else {
                    log.info("No eligible purchasers for invoice: {}", invoice.getId());
                    invoice.setInvoiceStatus(InvoiceStatus.NON_FINANCED.getDescription());
                }
            } else {
                log.error("Maturity date: {} for invoice: {} is before the current date: {}. Marking invoice as canceled.", invoice.getMaturityDate(), invoice.getId(), currentDate);
                invoice.setInvoiceStatus(InvoiceStatus.CANCELED.getDescription());
            }

            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.error(e.getMessage());
            invoice.setInvoiceStatus(InvoiceStatus.CANCELED.getDescription());
            invoiceRepository.save(invoice);
        }
    }


    private void performFinancing(Invoice invoice, Pair<Purchaser, Integer> selectedPurchaserAndRate) {
        Purchaser selectedPurchaser = selectedPurchaserAndRate.getFirst();
        int financingRate = selectedPurchaserAndRate.getSecond();

        // Calculate the early payment amount
        long earlyPaymentAmount = invoice.getValueInCents() - financingRate;

        invoice.setInvoiceStatus(InvoiceStatus.FINANCED.getDescription());

        FinancingResult financingResult = FinancingResult.builder()
                .invoiceId(invoice.getId())
                .initialAmount(invoice.getValueInCents())
                .earlyPaymentAmount(earlyPaymentAmount)
                .financingDate(LocalDate.now().plusDays(selectedPurchaserAndRate.getFirst().getMinimumFinancingTermInDays()))
                .createdAt(LocalDateTime.now())
                .financingRate(financingRate)
                .purchaser(selectedPurchaser)
                .build();

        financingResultRepository.save(financingResult);
    }


    private int calculateFinancingTerm(LocalDate maturityDate) {
        // Calculate the number of days between the two dates
        Period period = Period.between(LocalDate.now(), maturityDate);
        return period.getDays();

    }

    private Pair<Purchaser, Integer> selectPurchaser(List<Purchaser> eligiblePurchasers, Creditor creditor, int financingTermInDays) {
        // Find the eligible Purchaser with the minimum financing rate for the given Creditor
        return eligiblePurchasers.stream()
                .map(purchaser -> {
                    PurchaserFinancingSettings settings = purchaser.getPurchaserFinancingSettings().stream()
                            .filter(e -> Objects.equals(e.getCreditor(), creditor))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Creditor " + creditor.getName() + " doesn't exist for Purchaser: " + purchaser.getName()));

                    // Calculate the financing rate in basis points (bps)
                    int financingRateInBps = (settings.getAnnualRateInBps() * financingTermInDays) / 360;

                    // Return a Pair of Purchaser and their financing rate
                    return Pair.of(purchaser, financingRateInBps);
                })
                // Find the Purchaser with the minimum financing rate
                .min(Comparator.comparingInt(Pair::getSecond))
                //TODO Wrong exception thrown. Custom exception needed here.
                .orElseThrow(() -> new IllegalStateException("No eligible Purchaser found for Creditor: " + creditor.getName()));
    }


}
