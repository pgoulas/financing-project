package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
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
                List<Purchaser> eligiblePurchasers = getEligiblePurchasers(purchasers, creditor, financingTermInDays);
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

    private List<Purchaser> getEligiblePurchasers(List<Purchaser> purchasers, Creditor creditor, int financingTermInDays) {
        return purchasers.stream()
                // 1. the `Purchaser` has set up the settings for the invoice's `Creditor` (has a `PurchaserFinancingSettings`
                //  defined for this `Creditor`);
                .filter(purchaser -> purchaser.getPurchaserFinancingSettings().stream()
                        .anyMatch(purchaserFinancingSetting -> Objects.equals(purchaserFinancingSetting.getCreditor(), creditor)))
                //  2. the financing term of the invoice (duration between the current date and the maturity date of the invoice)
                //  is greater or equal to the value `Purchaser.minimumFinancingTermInDays` for this `Purchaser`;
                .filter(purchaser -> purchaser.getMinimumFinancingTermInDays() >= financingTermInDays)
                // 3. the `Purchaser`'s financing rate for the invoice doesn't exceed the `Creditor.maxFinancingRateInBps` value
                //for the invoice's `Creditor`.
                .filter(purchaser -> {
                    List<PurchaserFinancingSettings> purchaserFinancingSettings = purchaser.getPurchaserFinancingSettings().stream()
                            .filter(e -> Objects.equals(e.getCreditor(), creditor)).toList();
                    if(purchaserFinancingSettings.isEmpty()) {
                        return false;
                    } else if (purchaserFinancingSettings.size() == 1) {
                        int financingRate = (purchaserFinancingSettings.get(0).getAnnualRateInBps() * financingTermInDays) /360 ;
                        return financingRate > 0 && financingRate  <= creditor.getMaxFinancingRateInBps();
                    } else {
                        throw new RuntimeException("More than one entries for the same creditor");
                    }

                })
                .toList();
    }

    private Pair<Purchaser, Integer> selectPurchaser(List<Purchaser> eligiblePurchasers, Creditor creditor, int financingTermInDays) {
        //foreach eligible purchaser,we have to calculate the financing rate
        // in order to select the be

        // map with purchaser and annual rate for the given creditor
        Map<Purchaser, Integer> purchaserLongMap = new HashMap<>();
        eligiblePurchasers.forEach(purchaser -> {
            PurchaserFinancingSettings purchaserFinancingSettings = purchaser.getPurchaserFinancingSettings().stream()
                    .filter(e -> Objects.equals(e.getCreditor(), creditor))
                    .findFirst().orElseThrow(() -> new RuntimeException("Creditor " + creditor.getName() + " doesnt exist"));
            purchaserLongMap.put(purchaser, purchaserFinancingSettings.getAnnualRateInBps());
        });

        // calculate the financing rate for its purchaser
        Map<Purchaser, Integer> purchaserRateMap = new HashMap<>();
        purchaserLongMap.forEach((purchaser, integer) -> {
            int bps = (integer * financingTermInDays) / 360;
            purchaserRateMap.put(purchaser, bps);
        });

        // Find the minimum value in the map using streams
        Optional<Map.Entry<Purchaser, Integer>> minEntry = purchaserRateMap.entrySet().stream()
                .min(Map.Entry.comparingByValue());

        if (minEntry.isEmpty()) {
            throw new RuntimeException("Invalid action");
        }

        return Pair.of(minEntry.get().getKey(), minEntry.get().getValue());

    }

}
