package com.laundryhub.ui;

/**
 * Outline icons (stroke 2) copied from the design prototype. Colors follow currentColor.
 */
public final class Icons {

    private static final String OPEN = "<svg width=\"%s\" height=\"%s\" viewBox=\"0 0 24 24\" fill=\"none\" "
            + "stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">";
    private static final String CLOSE = "</svg>";

    /** Washing machine logo. */
    public static final String LOGO = svg(22, "<rect x=\"4\" y=\"2\" width=\"16\" height=\"20\" rx=\"3\"/>"
            + "<circle cx=\"12\" cy=\"13\" r=\"5\"/><path d=\"M8 6h.01M11 6h2\"/>");
    /** Notification bell. */
    public static final String BELL = svg(20, "<path d=\"M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9\"/>"
            + "<path d=\"M10.3 21a1.94 1.94 0 0 0 3.4 0\"/>");
    /** Arrow right (Start laundry order). */
    public static final String ARROW_RIGHT = svg(18, "<path d=\"M5 12h14M13 6l6 6-6 6\"/>");
    /** Back chevron. */
    public static final String BACK = svg(20, "<path d=\"M15 6l-6 6 6 6\"/>");
    /** Home tab. */
    public static final String HOME = svg(22, "<path d=\"M3 10l9-7 9 7v10a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z\"/>");
    /** Orders tab. */
    public static final String ORDERS = svg(22, "<rect x=\"5\" y=\"3\" width=\"14\" height=\"18\" rx=\"2\"/>"
            + "<path d=\"M9 8h6M9 12h6M9 16h3\"/>");
    /** Profile tab. */
    public static final String PROFILE = svg(22, "<circle cx=\"12\" cy=\"8\" r=\"4\"/><path d=\"M4 21c0-4 4-6 8-6s8 2 8 6\"/>");
    /** Shop (walk-in). */
    public static final String SHOP = svg(24, "<path d=\"M3 21h18M5 21V8l7-5 7 5v13M9 21v-6h6v6\"/>");
    /** Truck (pickup &amp; delivery, rider). */
    public static final String TRUCK = svg(24, "<path d=\"M1 6h13v10H1zM14 10h4l3 3v3h-7\"/>"
            + "<circle cx=\"5.5\" cy=\"18\" r=\"2\"/><circle cx=\"17.5\" cy=\"18\" r=\"2\"/>");
    /** Alert circle (minimum load note). */
    public static final String ALERT = svg(18, "<circle cx=\"12\" cy=\"12\" r=\"10\"/><path d=\"M12 8v4M12 16h.01\"/>");
    /** Soap drop. */
    public static final String DROP = svg(18, "<path d=\"M12 2.7s6 6.3 6 11.3a6 6 0 0 1-12 0c0-5 6-11.3 6-11.3z\"/>");
    /** Map pin. */
    public static final String PIN = svg(20, "<path d=\"M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z\"/>"
            + "<circle cx=\"12\" cy=\"9.5\" r=\"2.5\"/>");
    /** Check mark. */
    public static final String CHECK = svg(18, "<path d=\"M5 12l5 5 9-10\"/>");
    /** Phone (call rider/customer). */
    public static final String PHONE = svg(20, "<path d=\"M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 "
            + "19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 "
            + "2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.5c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2z\"/>");
    /** Menu (admin top bar on phones). */
    public static final String MENU = svg(22, "<path d=\"M4 6h16M4 12h16M4 18h16\"/>");
    /** Close. */
    public static final String CLOSE_X = svg(20, "<path d=\"M6 6l12 12M18 6L6 18\"/>");
    /** Location target (use my current location). */
    public static final String LOCATE = svg(20, "<circle cx=\"12\" cy=\"12\" r=\"7\"/><circle cx=\"12\" cy=\"12\" r=\"2\"/>"
            + "<path d=\"M12 2v3M12 19v3M2 12h3M19 12h3\"/>");
    /** Download (export CSV, install app). */
    public static final String DOWNLOAD = svg(18, "<path d=\"M12 3v12M7 10l5 5 5-5M5 21h14\"/>");

    /** Google "G" logo in its brand colors (not an outline icon). */
    public static final String GOOGLE = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 48 48\">"
            + "<path fill=\"#FFC107\" d=\"M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 "
            + "12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z\"/>"
            + "<path fill=\"#FF3D00\" d=\"M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 "
            + "16.3 4 9.7 8.3 6.3 14.7z\"/>"
            + "<path fill=\"#4CAF50\" d=\"M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-8l-6.5 "
            + "5C9.5 39.6 16.2 44 24 44z\"/>"
            + "<path fill=\"#1976D2\" d=\"M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z\"/>"
            + "</svg>";

    private Icons() {
    }

    private static String svg(int size, String body) {
        String s = Integer.toString(size);
        return OPEN.replaceFirst("%s", s).replaceFirst("%s", s) + body + CLOSE;
    }
}
