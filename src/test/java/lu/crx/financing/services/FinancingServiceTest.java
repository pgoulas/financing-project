package lu.crx.financing.services;

import lu.crx.financing.repositories.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class FinancingServiceTest {

    @Autowired
    private InvoiceRepository invoiceRepository;


    @BeforeEach
    void setUp() {
        //seedingService.seedMasterData();
    }

    @Test
    void testSeedingData() {
        assertEquals(15, invoiceRepository.findAll().size());
    }
}
