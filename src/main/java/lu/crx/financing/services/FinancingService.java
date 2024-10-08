package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.FinancingResult;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.entities.InvoiceStatus;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
//import lu.crx.financing.repositories.FinancingResultRepository;
import lu.crx.financing.repositories.FinancingResultRepository;
import lu.crx.financing.repositories.InvoiceRepository;
import lu.crx.financing.repositories.PurchaserRepository;
import lu.crx.financing.utils.EligibilityCheckUtils;
import org.springframework.data.domain.Page;
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
        log.info("Financing started");
        //collect the invoices

        //todo: change the query in order to user batch size
        PageRequest pageRequest = PageRequest.of(0, 40);
        List<Invoice> invoices = invoiceRepository.findAllByInvoiceStatus(InvoiceStatus.PENDING, pageRequest).getContent();

        //todo: change a custom query in order to retrieve purchasers
        List<Purchaser> purchasers = purchaserRepository.findAll();

        invoices.forEach(invoice -> {
            LocalDate currentDate = LocalDate.now();
            if (invoice.getMaturityDate().isAfter(currentDate)) {
                log.info("Maturity date: {} is after the current date {}", invoice.getMaturityDate(), currentDate);

                // financing term
                int financingTermInDays = calculateFinancingTerm(invoice.getMaturityDate());

                // get creditor
                Creditor creditor = invoice.getCreditor();

                // get eligible purchasers
                List<Purchaser> eligiblePurchasers = eligibilityCheckUtils.getEligiblePurchasers(purchasers, creditor, financingTermInDays);
                log.info("Eligible purchasers: {} for invoice: {}", eligiblePurchasers.size(), invoice.getId());

                if (!eligiblePurchasers.isEmpty()) {
                    //select the final purchaser
                    Pair<Purchaser, Integer> selectedPurchaserAndRatePair = selectPurchaser(eligiblePurchasers, creditor, financingTermInDays);
                    log.info("purchaser selected: {} for invoice: {}", selectedPurchaserAndRatePair.getFirst(), selectedPurchaserAndRatePair.getSecond());

                    // calculate the early payment amount
                    long earlyPaymentAmount = invoice.getValueInCents() - selectedPurchaserAndRatePair.getSecond();

                    //select the financial rate
                    int financingRate = selectedPurchaserAndRatePair.getSecond();

                    invoice.setInvoiceStatus(InvoiceStatus.FINANCED);
                    invoice.setPurchaser(selectedPurchaserAndRatePair.getFirst());

                    FinancingResult financingResult = FinancingResult.builder()
                            .invoiceId(invoice.getId())
                            .initialAmount(invoice.getValueInCents())
                            .earlyPaymentAmount(earlyPaymentAmount)
                            .financingDate(LocalDate.now().plusDays(50))
                            .createdAt(LocalDateTime.now())
                            .financingRate(financingRate)
                            .build();

                    financingResultRepository.save(financingResult);
                } else {
                    log.info("No eligible purchasers for invoice: {}", invoice.getId());
                    invoice.setInvoiceStatus(InvoiceStatus.NON_FINANCED);
                }

            } else {
                log.error("Maturity date: {} for invoice: {} is before current date: {}. Invoice is faulty", invoice.getMaturityDate(), invoice.getId(), currentDate);
                invoice.setInvoiceStatus(InvoiceStatus.CANCELED);
            }
            invoiceRepository.save(invoice);
        });

        // TODO This is the financing algorithm that needs to be implemented according to the specification.

        log.info("Financing completed");
    }

    public void fetchInvoicesInBatches(int batchSize) {
        int pageNumber = 0;
        Page<Invoice> invoicePage;

        do {
            // Create a Pageable object with the batch size and current page number
            PageRequest pageRequest = PageRequest.of(pageNumber, batchSize);

            // Fetch a page of invoices
            invoicePage = invoiceRepository.findAll(pageRequest);

            // Process the invoices in the current batch
            List<Invoice> invoices = invoicePage.getContent();
            invoices.forEach(invoice -> {
                // Process each invoice here
                System.out.println(invoice);
            });

            // Move to the next page
            pageNumber++;
        } while (invoicePage.hasNext());
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
                .orElseThrow(() -> new IllegalStateException("No eligible Purchaser found for Creditor: " + creditor.getName()));
    }


}
