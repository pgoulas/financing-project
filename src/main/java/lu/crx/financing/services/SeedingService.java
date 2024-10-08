package lu.crx.financing.services;

import java.time.LocalDate;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.Debtor;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.enums.InvoiceStatus;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SeedingService {

    private final EntityManager entityManager;

    private Creditor creditor1;
    private Creditor creditor2;
    private Creditor creditor3;

    private Debtor debtor1;
    private Debtor debtor2;
    private Debtor debtor3;

    private Purchaser purchaser1;
    private Purchaser purchaser2;
    private Purchaser purchaser3;

    public SeedingService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void seedMasterData() {
        log.info("Seeding master data");

        // creditors
        creditor1 = Creditor.builder()
                .name("Coffee Beans LLC")
                .maxFinancingRateInBps(5)
                .build();
        entityManager.persist(creditor1);

        creditor2 = Creditor.builder()
                .name("Home Brew")
                .maxFinancingRateInBps(3)
                .build();
        entityManager.persist(creditor2);

        creditor3 = Creditor.builder()
                .name("Beanstalk")
                .maxFinancingRateInBps(2)
                .build();
        entityManager.persist(creditor3);

        // debtors
        debtor1 = Debtor.builder()
                .name("Chocolate Factory")
                .build();
        entityManager.persist(debtor1);

        debtor2 = Debtor.builder()
                .name("Sweets Inc")
                .build();
        entityManager.persist(debtor2);

        debtor3 = Debtor.builder()
                .name("ChocoLoco")
                .build();
        entityManager.persist(debtor3);

        // purchasers
        purchaser1 = Purchaser.builder()
                .name("RichBank")
                .minimumFinancingTermInDays(10)
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor1)
                        .annualRateInBps(50)
                        .build())
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor2)
                        .annualRateInBps(60)
                        .build())
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor3)
                        .annualRateInBps(30)
                        .build())
                .build();
        entityManager.persist(purchaser1);

        purchaser2 = Purchaser.builder()
                .name("FatBank")
                .minimumFinancingTermInDays(12)
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor1)
                        .annualRateInBps(40)
                        .build())
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor2)
                        .annualRateInBps(80)
                        .build())
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor3)
                        .annualRateInBps(25)
                        .build())
                .build();
        entityManager.persist(purchaser2);

        purchaser3 = Purchaser.builder()
                .name("MegaBank")
                .minimumFinancingTermInDays(8)
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor1)
                        .annualRateInBps(30)
                        .build())
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor2)
                        .annualRateInBps(50)
                        .build())
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor3)
                        .annualRateInBps(45)
                        .build())
                .build();
        entityManager.persist(purchaser3);
    }

    @Transactional
    public void seedInvoices() {
        log.info("Seeding the invoices");

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor1)
                .valueInCents(200000)
                .maturityDate(LocalDate.now().plusDays(52))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor2)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(33))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor3)
                .valueInCents(600000)
                .maturityDate(LocalDate.now().plusDays(43))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor1)
                .valueInCents(500000)
                .maturityDate(LocalDate.now().plusDays(80))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor2)
                .valueInCents(6000000)
                .maturityDate(LocalDate.now().plusDays(5))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor3)
                .valueInCents(500000)
                .maturityDate(LocalDate.now().plusDays(10))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor1)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(15))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor2)
                .valueInCents(9000000)
                .maturityDate(LocalDate.now().plusDays(30))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor3)
                .valueInCents(450000)
                .maturityDate(LocalDate.now().plusDays(32))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor1)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(11))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor2)
                .valueInCents(3000000)
                .maturityDate(LocalDate.now().plusDays(10))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor3)
                .valueInCents(5000000)
                .maturityDate(LocalDate.now().plusDays(14))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor1)
                .valueInCents(9000000)
                .maturityDate(LocalDate.now().plusDays(23))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor2)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(18))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor3)
                .valueInCents(9000000)
                .maturityDate(LocalDate.now().plusDays(50))
                .invoiceStatus(InvoiceStatus.PENDING)
                .build());
    }

}
