package com.laundryhub.util;

/**
 * Date/time helpers without java.time (kept small and TeaVM-friendly).
 * The shop runs on Philippine time (UTC+8, no daylight saving).
 */
public final class TimeUtils {

    /** Philippine time offset in milliseconds (UTC+8). */
    public static final long MANILA_OFFSET_MILLIS = 8L * 60 * 60 * 1000;

    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;
    private static final long MILLIS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int DAYS_PER_ERA = 146097;
    private static final int YEARS_PER_ERA = 400;
    private static final int UNIX_EPOCH_SHIFT_DAYS = 719468;

    private TimeUtils() {
    }

    /**
     * @param epochMillis time in milliseconds since 1970 (UTC)
     * @return ISO-8601 UTC text, e.g. 2026-10-20T03:15:00Z
     */
    public static String isoUtc(long epochMillis) {
        long days = Math.floorDiv(epochMillis, MILLIS_PER_DAY);
        long secOfDay = Math.floorMod(epochMillis, MILLIS_PER_DAY) / MILLIS_PER_SECOND;
        int[] ymd = civilFromDays(days);
        long h = secOfDay / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR);
        long m = (secOfDay / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
        long s = secOfDay % SECONDS_PER_MINUTE;
        return date(ymd) + "T" + two(h) + ":" + two(m) + ":" + two(s) + "Z";
    }

    /**
     * @param epochMillis time in milliseconds since 1970 (UTC)
     * @return the Philippine calendar date, e.g. 2026-10-20
     */
    public static String manilaDate(long epochMillis) {
        return date(civilFromDays(Math.floorDiv(epochMillis + MANILA_OFFSET_MILLIS, MILLIS_PER_DAY)));
    }

    /**
     * @param epochMillis time in milliseconds since 1970 (UTC)
     * @param days        days to add (may be negative)
     * @return the Philippine date that many days later
     */
    public static String manilaDatePlusDays(long epochMillis, int days) {
        return manilaDate(epochMillis + days * MILLIS_PER_DAY);
    }

    /**
     * Converts a database timestamp to a short Philippine time label like "Oct 20, 2:05 PM".
     *
     * @param iso timestamp from Postgres, e.g. 2026-10-20T06:05:00.123+00:00
     * @return label, or "" when it cannot be read
     */
    public static String manilaShortLabel(String iso) {
        long millis = parseIso(iso);
        if (millis == Long.MIN_VALUE) {
            return "";
        }
        long local = millis + MANILA_OFFSET_MILLIS;
        int[] ymd = civilFromDays(Math.floorDiv(local, MILLIS_PER_DAY));
        long secOfDay = Math.floorMod(local, MILLIS_PER_DAY) / MILLIS_PER_SECOND;
        long h24 = secOfDay / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR);
        long min = (secOfDay / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
        final int noon = 12;
        String ampm = h24 < noon ? "AM" : "PM";
        long h12 = h24 % noon == 0 ? noon : h24 % noon;
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[ymd[1] - 1] + " " + ymd[2] + ", " + h12 + ":" + two(min) + " " + ampm;
    }

    /**
     * @param iso ISO timestamp with "Z" or a "+hh:mm" offset
     * @return epoch millis, or Long.MIN_VALUE when it cannot be read
     */
    public static long parseIso(String iso) {
        final int minLength = 19;
        if (iso == null || iso.length() < minLength) {
            return Long.MIN_VALUE;
        }
        try {
            int y = Integer.parseInt(iso.substring(0, 4));
            int mo = Integer.parseInt(iso.substring(5, 7));
            int d = Integer.parseInt(iso.substring(8, 10));
            int h = Integer.parseInt(iso.substring(11, 13));
            int mi = Integer.parseInt(iso.substring(14, 16));
            int s = Integer.parseInt(iso.substring(17, 19));
            long offsetMinutes = 0;
            int sign = Math.max(iso.lastIndexOf('+'), iso.lastIndexOf('-'));
            if (sign > minLength - 1) {
                int oh = Integer.parseInt(iso.substring(sign + 1, sign + 3));
                int om = iso.length() >= sign + 6 ? Integer.parseInt(iso.substring(sign + 4, sign + 6)) : 0;
                offsetMinutes = (oh * MINUTES_PER_HOUR + om) * (iso.charAt(sign) == '-' ? -1 : 1);
            }
            long days = daysFromCivil(y, mo, d);
            long secs = days * (MILLIS_PER_DAY / MILLIS_PER_SECOND) + h * 3600L + mi * 60L + s - offsetMinutes * 60L;
            return secs * MILLIS_PER_SECOND;
        } catch (RuntimeException e) {
            return Long.MIN_VALUE;
        }
    }

    // Howard Hinnant's civil date algorithms.
    private static int[] civilFromDays(long z) {
        z += UNIX_EPOCH_SHIFT_DAYS;
        long era = Math.floorDiv(z, DAYS_PER_ERA);
        long doe = z - era * DAYS_PER_ERA;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * YEARS_PER_ERA;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        return new int[] {(int) (m <= 2 ? y + 1 : y), (int) m, (int) d};
    }

    private static long daysFromCivil(int year, int month, int day) {
        long y = month <= 2 ? year - 1 : year;
        long era = Math.floorDiv(y, YEARS_PER_ERA);
        long yoe = y - era * YEARS_PER_ERA;
        long doy = (153L * (month > 2 ? month - 3 : month + 9) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * DAYS_PER_ERA + doe - UNIX_EPOCH_SHIFT_DAYS;
    }

    private static String date(int[] ymd) {
        return ymd[0] + "-" + two(ymd[1]) + "-" + two(ymd[2]);
    }

    private static String two(long v) {
        return v < 10 ? "0" + v : Long.toString(v);
    }
}
