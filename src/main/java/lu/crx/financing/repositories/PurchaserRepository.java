package lu.crx.financing.repositories;

import java.util.List;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.Purchaser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaserRepository extends JpaRepository<Purchaser, Long> {

    @Query("SELECT p FROM Purchaser p JOIN p.purchaserFinancingSettings ps WHERE ps.creditor = :creditor")
    List<Purchaser> findPurchasersByCreditor(@Param("creditor") Creditor creditor);
}
