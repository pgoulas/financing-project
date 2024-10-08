package lu.crx.financing.repositories;

import lu.crx.financing.entities.Invoice;
import lu.crx.financing.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByInvoiceStatus(InvoiceStatus invoiceStatus, PageRequest pageRequest);
}
