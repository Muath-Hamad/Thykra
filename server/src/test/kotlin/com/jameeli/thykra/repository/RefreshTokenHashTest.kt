package com.jameeli.thykra.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RefreshTokenHashTest {

    @Test
    fun hash_matches_known_sha256_vector() {
        // SHA-256("abc") — pins the algorithm so a swap silently invalidating
        // every stored refresh token can't slip through.
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            RefreshTokenRepository.hashToken("abc")
        )
    }

    @Test
    fun hash_is_64_lowercase_hex_chars() {
        val hash = RefreshTokenRepository.hashToken("any-token-value")
        assertTrue(Regex("^[0-9a-f]{64}$").matches(hash), "unexpected hash format: $hash")
    }

    @Test
    fun hash_is_deterministic_and_input_sensitive() {
        assertEquals(RefreshTokenRepository.hashToken("t1"), RefreshTokenRepository.hashToken("t1"))
        assertNotEquals(RefreshTokenRepository.hashToken("t1"), RefreshTokenRepository.hashToken("t2"))
    }
}
