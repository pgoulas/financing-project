package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
import lu.crx.financing.repositories.InvoiceRepository;
import lu.crx.financing.repositories.PurchaserRepository;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class FinancingService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaserRepository purchaserRepository;

    @Transactional
    public void finance() {
        log.info("Financing started");
        //collect the invoices
        List<Invoice> invoices = invoiceRepository.findAll();
        LocalDate currentDate = LocalDate.now();
        List<Purchaser> purchasers = purchaserRepository.findAll();
        invoices.forEach(invoice -> {
            if (invoice.getMaturityDate().isAfter(currentDate)) {
                log.info("Maturity date: " + invoice.getMaturityDate() + " is after the current date" + currentDate);
            }
            int financingTermInDays = calculateFinancingTerm(invoice.getMaturityDate());
            Creditor creditor = invoice.getCreditor();
            // financing rate
            List<Purchaser> eligiblePurchasers = purchasers.stream()
                    .filter(purchaser -> purchaser.getPurchaserFinancingSettings().stream()
                            .anyMatch(purchaserFinancingSetting -> Objects.equals(purchaserFinancingSetting.getCreditor(), creditor)))
                    .filter(purchaser -> purchaser.getMinimumFinancingTermInDays() >= financingTermInDays)
                    .toList();

            Pair<Purchaser, Integer> selectedPurchaserAndRatePair = selectPurchaser(eligiblePurchasers, creditor,financingTermInDays);

            log.info("Eligible purchasers: " + eligiblePurchasers.size() + " for invoice " + invoice.getId());


            log.info("Debtor: " + invoice.getDebtor() + " ,Creditor: " + invoice.getCreditor() + ",Maturity Date:" + invoice.getMaturityDate() + ",Value:" + invoice.getValueInCents());
        });

        // TODO This is the financing algorithm that needs to be implemented according to the specification.

        log.info("Financing completed");
    }


    int calculateFinancingTerm(LocalDate maturityDate) {
        // Calculate the number of days between the two dates
        Period period = Period.between(LocalDate.now(), maturityDate);
        return period.getDays();

    }

    Pair<Purchaser, Integer> selectPurchaser(List<Purchaser> eligiblePurchasers, Creditor creditor, int financingTermInDays) {
        //foreach eligible purchaser,we have to calculate the financing rate
        // in order to select the be

        // map with purchaser and annual rate for the given creditor
        Map<Purchaser, Integer> purchaserLongMap = new HashMap<>();
                eligiblePurchasers.forEach(purchaser -> {
                    PurchaserFinancingSettings purchaserFinancingSettings = purchaser.getPurchaserFinancingSettings().stream()
                            .filter(e -> Objects.equals(e.getCreditor(), creditor))
                            .findFirst().orElseThrow(() ->  new RuntimeException("Creditor" + creditor.getName() + "not exist"));
                    purchaserLongMap.put(purchaser, purchaserFinancingSettings.getAnnualRateInBps());
                });

        // calculate the financing rate for its purchaser
        Map<Purchaser, Integer> purchaserRateMap = new HashMap<>();
        purchaserLongMap.forEach((purchaser, integer) -> {
            int bps = (integer * financingTermInDays)/ 360;
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
