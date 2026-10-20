package com.laundryhub.util;

/**
 * Percent-encoding for URL query values (same result as JavaScript encodeURIComponent).
 * Pure Java, so request URLs can be unit tested.
 */
public final class UrlEncoder {

    private static final String HEX = "0123456789ABCDEF";
    private static final String UNRESERVED = "-_.!~*'()";
    private static final int ONE_BYTE_MAX = 0x7F;
    private static final int TWO_BYTE_MAX = 0x7FF;
    private static final int THREE_BYTE_MAX = 0xFFFF;

    private UrlEncoder() {
    }

    /**
     * @param value text to encode, may be null
     * @return encoded text ("" for null)
     */
    public static String encode(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int cp = value.codePointAt(i);
            i += Character.charCount(cp);
            if ((cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z') || (cp >= '0' && cp <= '9')
                    || (cp <= ONE_BYTE_MAX && UNRESERVED.indexOf(cp) >= 0)) {
                sb.append((char) cp);
            } else {
                appendUtf8(sb, cp);
            }
        }
        return sb.toString();
    }

    /**
     * Decodes percent-encoding ("+" is kept as a space, as in URL fragments from Supabase).
     *
     * @param value encoded text
     * @return decoded text
     */
    public static String decode(String value) {
        if (value == null) {
            return "";
        }
        // Turn any raw (unencoded) characters into %XX first, so only bytes remain.
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '+') {
                encoded.append("%20");
            } else if (c > ONE_BYTE_MAX) {
                appendUtf8(encoded, c);
            } else {
                encoded.append(c);
            }
        }
        String s = encoded.toString();
        byte[] bytes = new byte[s.length()];
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                bytes[n++] = (byte) Integer.parseInt(s.substring(i + 1, i + 3), HEX.length());
                i += 2;
            } else {
                bytes[n++] = (byte) c;
            }
        }
        return decodeUtf8(bytes, n);
    }

    private static void appendUtf8(StringBuilder sb, int cp) {
        if (cp <= ONE_BYTE_MAX) {
            appendByte(sb, cp);
        } else if (cp <= TWO_BYTE_MAX) {
            appendByte(sb, 0xC0 | (cp >> 6));
            appendByte(sb, 0x80 | (cp & 0x3F));
        } else if (cp <= THREE_BYTE_MAX) {
            appendByte(sb, 0xE0 | (cp >> 12));
            appendByte(sb, 0x80 | ((cp >> 6) & 0x3F));
            appendByte(sb, 0x80 | (cp & 0x3F));
        } else {
            appendByte(sb, 0xF0 | (cp >> 18));
            appendByte(sb, 0x80 | ((cp >> 12) & 0x3F));
            appendByte(sb, 0x80 | ((cp >> 6) & 0x3F));
            appendByte(sb, 0x80 | (cp & 0x3F));
        }
    }

    private static void appendByte(StringBuilder sb, int b) {
        sb.append('%').append(HEX.charAt((b >> 4) & 0xF)).append(HEX.charAt(b & 0xF));
    }

    private static String decodeUtf8(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            int b = bytes[i] & 0xFF;
            int cp;
            int extra;
            if (b < 0x80) {
                cp = b;
                extra = 0;
            } else if (b >= 0xF0) {
                cp = b & 0x07;
                extra = 3;
            } else if (b >= 0xE0) {
                cp = b & 0x0F;
                extra = 2;
            } else {
                cp = b & 0x1F;
                extra = 1;
            }
            i++;
            for (int k = 0; k < extra && i < length; k++, i++) {
                cp = (cp << 6) | (bytes[i] & 0x3F);
            }
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }
}
