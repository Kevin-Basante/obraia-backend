package com.obraia.backend.email;

/** Helpers shared by the services that write emails. */
public final class EmailText {

    private EmailText() {
    }

    /**
     * FRONTEND_URL may list several URLs separated by commas: the first one
     * is used in the links, without a final slash.
     */
    public static String firstUrl(String urls) {
        String first = urls.split(",")[0].trim();
        return first.endsWith("/") ? first.substring(0, first.length() - 1) : first;
    }

    /** Text typed by users (names, project names) is escaped before going into HTML. */
    public static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
