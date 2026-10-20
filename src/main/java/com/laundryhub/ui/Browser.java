package com.laundryhub.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;

/**
 * Small browser helpers: the app's own address and leaving the page.
 */
public final class Browser {

    private Browser() {
    }

    /**
     * Address of the app without the hash, e.g. "https://laundry-hub.vercel.app/".
     * Used as the redirect for Google sign-in and e-mail links (must be in Supabase's redirect URLs).
     *
     * @return origin + path
     */
    @JSBody(script = "return window.location.origin + window.location.pathname;")
    public static native String appUrl();

    /**
     * @param url page to open in this tab (e.g. Google sign-in)
     */
    public static void go(String url) {
        Window.current().getLocation().assign(url);
    }

    /**
     * Saves text as a file (e.g. the sales CSV) using a Blob link.
     *
     * @param fileName file name, e.g. "laundryhub-sales.csv"
     * @param content  file content
     * @param mimeType e.g. "text/csv;charset=utf-8"
     */
    @JSBody(params = {"fileName", "content", "mimeType"}, script = ""
            + "var lhBlob = new Blob([content], { type: mimeType });"
            + "var lhUrl = URL.createObjectURL(lhBlob);"
            + "var lhLink = document.createElement('a');"
            + "lhLink.href = lhUrl; lhLink.download = fileName; document.body.appendChild(lhLink); lhLink.click();"
            + "setTimeout(function () { URL.revokeObjectURL(lhUrl); lhLink.remove(); }, 1000);")
    public static native void download(String fileName, String content, String mimeType);

    /**
     * Copies text to the clipboard (falls back to a hidden text box on older browsers).
     *
     * @param text text to copy, e.g. the shop's GCash number
     */
    @JSBody(params = "text", script = ""
            + "if (navigator.clipboard && window.isSecureContext) { navigator.clipboard.writeText(text); return; }"
            + "var lhArea = document.createElement('textarea'); lhArea.value = text;"
            + "lhArea.style.position = 'fixed'; lhArea.style.opacity = '0'; document.body.appendChild(lhArea);"
            + "lhArea.select(); try { document.execCommand('copy'); } catch (lhErr) { } lhArea.remove();")
    public static native void copyText(String text);

    /**
     * @param url page to open in a new tab (e.g. Google Maps directions)
     */
    public static void openNewTab(String url) {
        Window.current().open(url, "_blank");
    }
}
