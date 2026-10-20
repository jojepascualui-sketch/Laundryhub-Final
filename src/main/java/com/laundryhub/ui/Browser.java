package com.laundryhub.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLInputElement;

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
     * Saves a file from another site (e.g. the GCash QR in Supabase Storage) under a given name.
     * Opens it in a new tab when the browser cannot download it directly.
     *
     * @param url      file address
     * @param fileName name for the saved file
     */
    @JSBody(params = {"url", "fileName"}, script = ""
            + "fetch(url).then(function (lhRes) { if (!lhRes.ok) throw new Error(); return lhRes.blob(); })"
            + ".then(function (lhBlob) {"
            + "  var lhUrl = URL.createObjectURL(lhBlob); var lhLink = document.createElement('a');"
            + "  lhLink.href = lhUrl; lhLink.download = fileName; document.body.appendChild(lhLink); lhLink.click();"
            + "  setTimeout(function () { URL.revokeObjectURL(lhUrl); lhLink.remove(); }, 1000);"
            + "}).catch(function () { window.open(url, '_blank'); });")
    public static native void saveFile(String url, String fileName);

    /**
     * @param input a file input
     * @return the chosen File, or null
     */
    public static Object selectedFile(HTMLInputElement input) {
        return input == null ? null : selectedFileJs(input);
    }

    /**
     * @param file a File from {@link #selectedFile}
     * @return its MIME type (image/jpeg when unknown)
     */
    public static String fileType(Object file) {
        return fileTypeJs((JSObject) file);
    }

    /**
     * @param file a File from {@link #selectedFile}
     * @return its size in bytes
     */
    public static double fileSize(Object file) {
        return fileSizeJs((JSObject) file);
    }

    @JSBody(params = "input", script = "return (input && input.files && input.files.length) ? input.files[0] : null;")
    private static native JSObject selectedFileJs(HTMLInputElement input);

    @JSBody(params = "file", script = "return file.type || 'image/jpeg';")
    private static native String fileTypeJs(JSObject file);

    @JSBody(params = "file", script = "return file.size || 0;")
    private static native double fileSizeJs(JSObject file);

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
