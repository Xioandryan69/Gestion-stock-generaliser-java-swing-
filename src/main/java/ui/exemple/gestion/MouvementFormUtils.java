package ui.exemple.gestion;

import java.awt.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.swing.JOptionPane;

final class MouvementFormUtils {

    private MouvementFormUtils() {
    }

    static BigDecimal parseBigDecimal(Component parent, String text, String label) {
        try {
            String value = text == null ? "" : text.trim();
            if (value.isEmpty()) {
                throw new NumberFormatException("vide");
            }
            return new BigDecimal(value);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(parent,
                    "La " + label + " est invalide.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return "";
    }

    static LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
    }

    static java.util.Date toDate(LocalDate date) {
        return java.sql.Date.valueOf(date);
    }
}
