package com.smartcloudbrain.common.security;

import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TextCryptoService {

  private static final String PREFIX = "{aes-gcm}";
  private static final int IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;

  private final SecureRandom secureRandom = new SecureRandom();
  private final SecretKeySpec keySpec;

  public TextCryptoService(
      @Value("${smtp-config.secret:${SMTP_CONFIG_SECRET:${JWT_SECRET:smart-cloud-brain-local-secret-please-change}}}") String secret
  ) {
    this.keySpec = new SecretKeySpec(sha256(secret), "AES");
  }

  public String encrypt(String plainText) {
    if (plainText == null || plainText.isBlank()) {
      throw new BusinessException(400, "secret value must not be blank");
    }
    try {
      byte[] iv = new byte[IV_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      ByteBuffer payload = ByteBuffer.allocate(iv.length + encrypted.length);
      payload.put(iv);
      payload.put(encrypted);
      return PREFIX + Base64.getEncoder().encodeToString(payload.array());
    } catch (Exception ex) {
      throw new BusinessException(500, "unable to encrypt secret value");
    }
  }

  public String decrypt(String storedText) {
    if (storedText == null || storedText.isBlank()) {
      return "";
    }
    if (!storedText.startsWith(PREFIX)) {
      throw new BusinessException(500, "unsupported encrypted secret format");
    }
    try {
      byte[] payload = Base64.getDecoder().decode(storedText.substring(PREFIX.length()));
      ByteBuffer buffer = ByteBuffer.wrap(payload);
      byte[] iv = new byte[IV_BYTES];
      buffer.get(iv);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new BusinessException(500, "unable to decrypt secret value");
    }
  }

  private byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to derive encryption key", ex);
    }
  }
}
