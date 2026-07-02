package com.smartcloudbrain.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartcloudbrain.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextCryptoServiceTest {

  private TextCryptoService service;

  @BeforeEach
  void setUp() {
    service = new TextCryptoService("test-secret-key-for-encryption");
  }

  // ── encrypt / decrypt roundtrip ─────────────────────────────────

  @Test
  void encryptThenDecryptReturnsOriginalText() {
    String plain = "hello world";
    String encrypted = service.encrypt(plain);

    assertTrue(encrypted.startsWith("{aes-gcm}"));
    assertNotEquals(plain, encrypted);

    String decrypted = service.decrypt(encrypted);
    assertEquals(plain, decrypted);
  }

  @Test
  void encryptThenDecryptHandlesUnicodeText() {
    String plain = "中文测试 émojis 🎉";
    String encrypted = service.encrypt(plain);

    String decrypted = service.decrypt(encrypted);
    assertEquals(plain, decrypted);
  }

  @Test
  void encryptThenDecryptHandlesLongText() {
    String plain = "A".repeat(10_000);
    String encrypted = service.encrypt(plain);

    String decrypted = service.decrypt(encrypted);
    assertEquals(plain, decrypted);
  }

  // ── encrypt edge cases ──────────────────────────────────────────

  @Test
  void encryptThrowsOnNullInput() {
    assertThrows(BusinessException.class, () -> service.encrypt(null));
  }

  @Test
  void encryptThrowsOnBlankInput() {
    assertThrows(BusinessException.class, () -> service.encrypt(""));
    assertThrows(BusinessException.class, () -> service.encrypt("   "));
  }

  // ── decrypt edge cases ──────────────────────────────────────────

  @Test
  void decryptReturnsEmptyForNullInput() {
    assertEquals("", service.decrypt(null));
  }

  @Test
  void decryptReturnsEmptyForBlankInput() {
    assertEquals("", service.decrypt(""));
    assertEquals("", service.decrypt("   "));
  }

  @Test
  void decryptThrowsOnMissingPrefix() {
    assertThrows(BusinessException.class, () -> service.decrypt("not-encrypted"));
  }

  @Test
  void decryptThrowsOnCorruptedPayload() {
    assertThrows(BusinessException.class, () -> service.decrypt("{aes-gcm}!!!invalid-base64!!!"));
  }

  @Test
  void decryptThrowsOnTamperedCiphertext() {
    String encrypted = service.encrypt("secret");
    // Tamper with the base64 payload by changing a character
    String tampered = encrypted.substring(0, encrypted.length() - 2) + "XX";
    assertThrows(BusinessException.class, () -> service.decrypt(tampered));
  }

  // ── different secrets produce different ciphertext ───────────────

  @Test
  void differentSecretsCannotDecryptEachOther() {
    TextCryptoService other = new TextCryptoService("different-secret-key");
    String encrypted = service.encrypt("hello");

    assertThrows(BusinessException.class, () -> other.decrypt(encrypted));
  }

  // ── same plaintext produces different ciphertext (random IV) ────

  @Test
  void samePlaintextProducesDifferentCiphertext() {
    String c1 = service.encrypt("same");
    String c2 = service.encrypt("same");
    assertNotEquals(c1, c2, "AES-GCM with random IV should produce different ciphertext each time");
  }

  // ── null secret in constructor ──────────────────────────────────

  @Test
  void constructorWithNullSecretStillWorks() {
    TextCryptoService nullSecretService = new TextCryptoService(null);
    String encrypted = nullSecretService.encrypt("test");
    assertEquals("test", nullSecretService.decrypt(encrypted));
  }
}
