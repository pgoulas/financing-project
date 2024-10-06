package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.repositories.InvoiceRepository;
import lu.crx.financing.repositories.PurchaserRepository;
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
        LocalDate localDate = LocalDate.now();
        invoices.forEach(invoice -> {
            try {
                if (invoice.getMaturityDate().isAfter(localDate)) {
                    log.info("Maturity date: " + invoice.getMaturityDate() + " is after the current date" + localDate);
                }
                log.info("Debtor: " + invoice.getDebtor() + " ,Creditor: " + invoice.getCreditor() + ",Maturity Date:" + invoice.getMaturityDate() + ",Value:" + invoice.getValueInCents());
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // TODO This is the financing algorithm that needs to be implemented according to the specification.

        log.info("Financing completed");
    }

}
