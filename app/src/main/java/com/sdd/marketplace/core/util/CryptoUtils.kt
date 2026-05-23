package com.sdd.marketplace.core.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption for sensitive data (KYC documents, PII).
 *
 * Uses Android Keystore for key management — keys are hardware-backed
 * where available and never leave the secure enclave.
 *
 * Usage:
 *   val encrypted = CryptoUtils.encrypt(KEY_KYC, sensitiveBytes)
 *   val decrypted = CryptoUtils.decrypt(KEY_KYC, encrypted)
 */
object CryptoUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    const val KEY_KYC = "sdd_kyc_key"
    const val KEY_USER_PII = "sdd_user_pii_key"
    const val KEY_PAYMENT = "sdd_payment_key"

    /**
     * Encrypt bytes using AES-256-GCM.
     * Returns Base64-encoded string: IV (12 bytes) + ciphertext.
     */
    fun encrypt(keyAlias: String, plaintext: ByteArray): String {
        val key = getOrCreateKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Encrypt a String (UTF-8) and return Base64-encoded ciphertext.
     */
    fun encryptString(keyAlias: String, plaintext: String): String =
        encrypt(keyAlias, plaintext.toByteArray(Charsets.UTF_8))

    /**
     * Decrypt Base64-encoded ciphertext back to bytes.
     */
    fun decrypt(keyAlias: String, encoded: String): ByteArray {
        val key = getOrCreateKey(keyAlias)
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * Decrypt to UTF-8 String.
     */
    fun decryptString(keyAlias: String, encoded: String): String =
        decrypt(keyAlias, encoded).toString(Charsets.UTF_8)

    /**
     * Hash a password using SHA-256. For password hashing bcrypt is used server-side;
     * this is used only for client-side integrity checks.
     */
    fun hashSha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Securely wipe a ByteArray from memory.
     */
    fun wipe(data: ByteArray) {
        data.fill(0)
    }

    private fun getOrCreateKey(keyAlias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        val existingKey = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
