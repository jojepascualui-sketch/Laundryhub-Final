package com.laundryhub.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MoneyFormatterTest {

    @Test
    void formatsSmallAmountWithTwoDecimals() {
        assertEquals("₱30.00", MoneyFormatter.formatPesos(30));
    }

    @Test
    void formatsCentavos() {
        assertEquals("₱410.05", MoneyFormatter.format(41005));
    }

    @Test
    void groupsThousandsWithCommas() {
        assertEquals("₱2,375.00", MoneyFormatter.formatPesos(2375));
        assertEquals("₱1,234,567.89", MoneyFormatter.format(123456789));
    }

    @Test
    void formatsZero() {
        assertEquals("₱0.00", MoneyFormatter.format(0));
    }

    @Test
    void formatsNegative() {
        assertEquals("-₱60.00", MoneyFormatter.formatPesos(-60));
    }
}
