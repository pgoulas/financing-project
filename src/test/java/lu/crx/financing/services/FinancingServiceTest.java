package lu.crx.financing.services;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.enums.InvoiceStatus;
import lu.crx.financing.repositories.CreditorRepository;
import lu.crx.financing.repositories.DebtorRepository;
import lu.crx.financing.repositories.FinancingResultRepository;
import lu.crx.financing.repositories.InvoiceRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinancingServiceTest {

    @Autowired
    private FinancingResultRepository financingResultRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private FinancingService financingService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DebtorRepository debtorRepository;

    @Autowired
    private CreditorRepository creditorRepository;

    @Test
    @Order(1)
    void testValidInvoicesProcessed() {
        assertEquals(11, financingResultRepository.findAll().size());
        assertEquals(15, invoiceRepository.findAll().size());

        assertEquals(1, financingResultRepository.findAll().get(0).getInvoiceId());
        assertEquals(3, financingResultRepository.findAll().get(1).getInvoiceId());
        assertEquals(4, financingResultRepository.findAll().get(2).getInvoiceId());
        assertEquals(6, financingResultRepository.findAll().get(3).getInvoiceId());
        assertEquals(7, financingResultRepository.findAll().get(4).getInvoiceId());
        assertEquals(10, financingResultRepository.findAll().get(5).getInvoiceId());
        assertEquals(11, financingResultRepository.findAll().get(6).getInvoiceId());
        assertEquals(12, financingResultRepository.findAll().get(7).getInvoiceId());
        assertEquals(13, financingResultRepository.findAll().get(8).getInvoiceId());
        assertEquals(14, financingResultRepository.findAll().get(9).getInvoiceId());
        assertEquals(15, financingResultRepository.findAll().get(10).getInvoiceId());

        assertEquals(199999, financingResultRepository.findAll().get(0).getEarlyPaymentAmount());
        assertEquals(599999, financingResultRepository.findAll().get(1).getEarlyPaymentAmount());
        assertEquals(499999, financingResultRepository.findAll().get(2).getEarlyPaymentAmount());
        assertEquals(499999, financingResultRepository.findAll().get(3).getEarlyPaymentAmount());
        assertEquals(799998, financingResultRepository.findAll().get(4).getEarlyPaymentAmount());
        assertEquals(799999, financingResultRepository.findAll().get(5).getEarlyPaymentAmount());
        assertEquals(2999999, financingResultRepository.findAll().get(6).getEarlyPaymentAmount());
        assertEquals(4999999, financingResultRepository.findAll().get(7).getEarlyPaymentAmount());
        assertEquals(8999999, financingResultRepository.findAll().get(8).getEarlyPaymentAmount());
        assertEquals(799999, financingResultRepository.findAll().get(9).getEarlyPaymentAmount());
        assertEquals(8999999, financingResultRepository.findAll().get(10).getEarlyPaymentAmount());
    }

    @Sql(scripts = "classpath:drop_invoices.sql")
    @Test
    void testFaultyInvoicesAreCancelled() {
        entityManager.persist(Invoice.builder()
                .creditor(creditorRepository.findAll().get(0))
                .debtor(debtorRepository.findAll().get(0))
                .valueInCents(200000)
                .maturityDate(LocalDate.now().minusDays(200))
                .invoiceStatus(InvoiceStatus.PENDING.getDescription())
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditorRepository.findAll().get(1))
                .debtor(debtorRepository.findAll().get(1))
                .valueInCents(800000)
                .maturityDate(LocalDate.now().minusDays(200))
                .invoiceStatus(InvoiceStatus.PENDING.getDescription())
                .build());

        financingService.finance();
        assertEquals(InvoiceStatus.CANCELED.getDescription(), invoiceRepository.findAll().get(0).getInvoiceStatus());
        assertEquals(InvoiceStatus.CANCELED.getDescription(), invoiceRepository.findAll().get(1).getInvoiceStatus());
    }

    @Sql(scripts = "classpath:drop_invoices.sql")
    @Test
    void testInvoicesWithNoEligiblePurchasers() {
        entityManager.persist(Invoice.builder()
                .creditor(creditorRepository.findAll().get(0))
                .debtor(debtorRepository.findAll().get(1))
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(33))
                .invoiceStatus(InvoiceStatus.PENDING.getDescription())
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditorRepository.findAll().get(0))
                .debtor(debtorRepository.findAll().get(1))
                .valueInCents(6000000)
                .maturityDate(LocalDate.now().plusDays(5))
                .invoiceStatus(InvoiceStatus.PENDING.getDescription())
                .build());

        financingService.finance();
        assertEquals(InvoiceStatus.NON_FINANCED.getDescription(), invoiceRepository.findAll().get(0).getInvoiceStatus());
        assertEquals(InvoiceStatus.NON_FINANCED.getDescription(), invoiceRepository.findAll().get(1).getInvoiceStatus());
    }
}
