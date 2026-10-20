package com.laundryhub.service;

import com.laundryhub.util.TimeUtils;

/**
 * Date ranges for the Sales reports screen, in Philippine dates (YYYY-MM-DD, inclusive).
 */
public enum ReportPeriod {
    /** Today only. */
    TODAY("Today", 0),
    /** Today and the 6 days before. */
    LAST_7_DAYS("Last 7 days", 6),
    /** Today and the 29 days before. */
    LAST_30_DAYS("Last 30 days", 29),
    /** From the 1st of this month. */
    THIS_MONTH("This month", -1);

    private final String label;
    private final int daysBack;

    ReportPeriod(String label, int daysBack) {
        this.label = label;
        this.daysBack = daysBack;
    }

    /** @return button text */
    public String getLabel() {
        return label;
    }

    /**
     * @param nowMillis current time
     * @return first day of the range
     */
    public String from(long nowMillis) {
        String today = TimeUtils.manilaDate(nowMillis);
        if (this == THIS_MONTH) {
            return today.substring(0, today.length() - 2) + "01";
        }
        return TimeUtils.manilaDatePlusDays(nowMillis, -daysBack);
    }

    /**
     * @param nowMillis current time
     * @return last day of the range (today)
     */
    public String to(long nowMillis) {
        return TimeUtils.manilaDate(nowMillis);
    }
}
