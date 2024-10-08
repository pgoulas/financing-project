package lu.crx.financing.utils;

import java.util.List;
import java.util.Objects;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
import org.springframework.stereotype.Service;

@Service
public class EligibilityCheckUtils {

    public List<Purchaser> getEligiblePurchasers(List<Purchaser> purchasers, Creditor creditor, int financingTermInDays) {
        return purchasers.stream()
                // Filter Purchasers who have financing settings for the given Creditor
                .filter(purchaser -> hasFinancingSettingsForCreditor(purchaser, creditor))
                // Check if the Purchaser meets the minimum financing term requirement
                .filter(purchaser -> meetsMinimumFinancingTerm(purchaser, financingTermInDays))
                // Check if the Purchaser's financing rate does not exceed the Creditor's max rate
                .filter(purchaser -> hasValidFinancingRate(purchaser, creditor, financingTermInDays))
                .toList();
    }

    private boolean hasFinancingSettingsForCreditor(Purchaser purchaser, Creditor creditor) {
        return purchaser.getPurchaserFinancingSettings().stream()
                .anyMatch(setting -> Objects.equals(setting.getCreditor(), creditor));
    }

    private boolean meetsMinimumFinancingTerm(Purchaser purchaser, int financingTermInDays) {
        return purchaser.getMinimumFinancingTermInDays() <= financingTermInDays;
    }

    private boolean hasValidFinancingRate(Purchaser purchaser, Creditor creditor, int financingTermInDays) {
        List<PurchaserFinancingSettings> settingsForCreditor = purchaser.getPurchaserFinancingSettings().stream()
                .filter(setting -> Objects.equals(setting.getCreditor(), creditor))
                .toList();

        if (settingsForCreditor.isEmpty()) {
            return false;
        }

        // Ensure only one entry exists for the same creditor
        if (settingsForCreditor.size() > 1) {
            throw new IllegalStateException("Multiple financing settings found for the same creditor: " + creditor.getName());
        }

        // Calculate financing rate
        PurchaserFinancingSettings setting = settingsForCreditor.get(0);
        int financingRateInBps = (setting.getAnnualRateInBps() * financingTermInDays) / 360;

        return financingRateInBps > 0 && financingRateInBps <= creditor.getMaxFinancingRateInBps();
    }

}
