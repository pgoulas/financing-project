package lu.crx.financing.services;

import lu.crx.financing.repositories.FinancingResultRepository;
import lu.crx.financing.repositories.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "classpath:drop_invoices.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@TestPropertySource("classpath:application-test.properties")
class FinancingServiceTest {

    @Autowired
    private FinancingResultRepository financingResultRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void testWithInitialSeedingData() {
        assertEquals(8, financingResultRepository.findAll().size());
        assertEquals(15, invoiceRepository.findAll().size());

        assertEquals(1, financingResultRepository.findAll().get(0).getInvoiceId());
        assertEquals(3, financingResultRepository.findAll().get(1).getInvoiceId());
        assertEquals(4, financingResultRepository.findAll().get(2).getInvoiceId());
        assertEquals(11, financingResultRepository.findAll().get(3).getInvoiceId());
        assertEquals(12, financingResultRepository.findAll().get(4).getInvoiceId());
        assertEquals(13, financingResultRepository.findAll().get(5).getInvoiceId());
        assertEquals(14, financingResultRepository.findAll().get(6).getInvoiceId());
        assertEquals(15, financingResultRepository.findAll().get(7).getInvoiceId());

        assertEquals(199999, financingResultRepository.findAll().get(0).getEarlyPaymentAmount());
        assertEquals(599999, financingResultRepository.findAll().get(1).getEarlyPaymentAmount());
        assertEquals(499999, financingResultRepository.findAll().get(2).getEarlyPaymentAmount());
        assertEquals(2999999, financingResultRepository.findAll().get(3).getEarlyPaymentAmount());
        assertEquals(4999999, financingResultRepository.findAll().get(4).getEarlyPaymentAmount());
        assertEquals(8999999, financingResultRepository.findAll().get(5).getEarlyPaymentAmount());
        assertEquals(799999, financingResultRepository.findAll().get(6).getEarlyPaymentAmount());
        assertEquals(8999999, financingResultRepository.findAll().get(7).getEarlyPaymentAmount());
    }

    @Test
    void testInvoicesTableIsCleared() {
        assertEquals(0, invoiceRepository.findAll().size());
    }
}
