package com.obraia.backend.email;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmailTextTest {

    @Test
    void firstUrlIsUsedWithoutTheFinalSlash() {
        assertEquals("https://obraia-frontend.vercel.app",
                EmailText.firstUrl(" https://obraia-frontend.vercel.app/ ,http://localhost:5173"));
    }

    @Test
    void singleUrlIsKept() {
        assertEquals("http://localhost:5173", EmailText.firstUrl("http://localhost:5173"));
    }

    @Test
    void htmlCharactersAreEscaped() {
        assertEquals("Casa &lt;b&gt; &amp; &quot;Local&quot;", EmailText.escape("Casa <b> & \"Local\""));
    }
}
