package lu.crx.financing.utils;

import java.util.List;
import java.util.Objects;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
import lu.crx.financing.exception.InvoiceException;
import org.springframework.stereotype.Service;

@Service
public class EligibilityCheckUtils {

    /**
     * Method tha calculate the eligible purchasers based on the given purchasers, creditor and financing term
     *
     * @param purchasers          The purchasers related to the given creditor
     * @param creditor            The creditor that needs financing check
     * @param financingTermInDays The calculated financing term
     * @return The eligible purchasers
     */
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

    /**
     * Method that checks if the given purchaser has settings for the given creditor
     *
     * @param purchaser The purchaser
     * @param creditor  The creditor
     * @return true if purchaser has settings for the creditor, otherwise returns false
     */
    private boolean hasFinancingSettingsForCreditor(Purchaser purchaser, Creditor creditor) {
        return purchaser.getPurchaserFinancingSettings().stream()
                .anyMatch(setting -> Objects.equals(setting.getCreditor(), creditor));
    }

    /**
     * Method that checks if the given purchaser meets minimum financing term
     *
     * @param purchaser           The purchaser
     * @param financingTermInDays The calculated financing term
     * @return true if purchaser meets minimum financing term, otherwise returns false
     */
    private boolean meetsMinimumFinancingTerm(Purchaser purchaser, int financingTermInDays) {
        return purchaser.getMinimumFinancingTermInDays() <= financingTermInDays;
    }

    /**
     * Method that checks if the given purchaser has valid financing rate
     *
     * @param purchaser           The purchaser
     * @param creditor            The creditor
     * @param financingTermInDays The calculated financing term
     * @return true if purchaser has valid financing rate, otherwise returns false
     */
    private boolean hasValidFinancingRate(Purchaser purchaser, Creditor creditor, int financingTermInDays) {

        //filter the purchasers by given creditor
        List<PurchaserFinancingSettings> settingsForCreditor = purchaser.getPurchaserFinancingSettings().stream()
                .filter(setting -> Objects.equals(setting.getCreditor(), creditor))
                .toList();

        if (settingsForCreditor.isEmpty()) {
            return false;
        }

        // Ensure only one entry exists for the same creditor
        if (settingsForCreditor.size() > 1) {
            throw new InvoiceException("Multiple financing settings found for the same creditor: " + creditor.getName());
        }

        // Calculate financing rate
        PurchaserFinancingSettings setting = settingsForCreditor.get(0);
        int financingRateInBps = (setting.getAnnualRateInBps() * financingTermInDays) / 360;

        return financingRateInBps > 0 && financingRateInBps <= creditor.getMaxFinancingRateInBps();
    }

}
