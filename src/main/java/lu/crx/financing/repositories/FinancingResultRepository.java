package lu.crx.financing.repositories;

import lu.crx.financing.entities.FinancingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancingResultRepository extends JpaRepository<FinancingResult, Long> {
}
