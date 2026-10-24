package com.obraia.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void generatesDifferentUrlSafeTokens() {
        String first = TokenHasher.generateToken();
        String second = TokenHasher.generateToken();

        assertNotEquals(first, second);
        // 32 bytes in Base64 without padding = 43 characters.
        assertEquals(43, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void hashIsStableAndHexadecimal() {
        String hash = TokenHasher.hash("abc");

        assertEquals(hash, TokenHasher.hash("abc"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash);
    }

    @Test
    void differentTokensHaveDifferentHashes() {
        assertNotEquals(TokenHasher.hash("token-1"), TokenHasher.hash("token-2"));
    }
}
