package com.laundryhub.util;

/**
 * Formats peso amounts the way the design shows them: {@code ₱1,234.00}.
 * Pure Java (no browser APIs), so it runs in JUnit and in TeaVM.
 */
public final class MoneyFormatter {

    private static final char PESO = '₱';
    private static final int CENTS_PER_PESO = 100;
    private static final int DIGITS_PER_GROUP = 3;

    private MoneyFormatter() {
    }

    /**
     * Formats an amount given in centavos.
     *
     * @param centavos amount in centavos, e.g. {@code 41000} for ₱410.00
     * @return the formatted amount, e.g. {@code ₱410.00}; negatives get a leading minus
     */
    public static String format(long centavos) {
        boolean negative = centavos < 0;
        long abs = Math.abs(centavos);
        long pesos = abs / CENTS_PER_PESO;
        long cents = abs % CENTS_PER_PESO;

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        sb.append(PESO).append(groupThousands(pesos)).append('.');
        if (cents < 10) {
            sb.append('0');
        }
        sb.append(cents);
        return sb.toString();
    }

    /**
     * Formats a whole-peso amount, e.g. {@code 30} becomes {@code ₱30.00}.
     *
     * @param pesos amount in whole pesos
     * @return the formatted amount
     */
    public static String formatPesos(long pesos) {
        return format(pesos * CENTS_PER_PESO);
    }

    /**
     * Formats without decimals when the amount is whole, as in "₱30 /kg" or "+ ₱5 /kg".
     *
     * @param centavos amount in centavos
     * @return e.g. {@code ₱30} or {@code ₱7.50}
     */
    public static String formatShort(long centavos) {
        if (centavos % CENTS_PER_PESO != 0) {
            return format(centavos);
        }
        String sign = centavos < 0 ? "-" : "";
        return sign + PESO + groupThousands(Math.abs(centavos) / CENTS_PER_PESO);
    }

    /**
     * Converts a peso amount from the database (e.g. {@code 410.5}) to centavos.
     *
     * @param pesos amount in pesos
     * @return amount in centavos, rounded to the nearest centavo
     */
    public static long toCentavos(double pesos) {
        return Math.round(pesos * CENTS_PER_PESO);
    }

    /**
     * Converts centavos to pesos for sending to the database.
     *
     * @param centavos amount in centavos
     * @return amount in pesos
     */
    public static double toPesos(long centavos) {
        return centavos / (double) CENTS_PER_PESO;
    }

    private static String groupThousands(long value) {
        String digits = Long.toString(value);
        StringBuilder sb = new StringBuilder();
        int firstGroup = digits.length() % DIGITS_PER_GROUP;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - firstGroup) % DIGITS_PER_GROUP == 0) {
                sb.append(',');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }
}
