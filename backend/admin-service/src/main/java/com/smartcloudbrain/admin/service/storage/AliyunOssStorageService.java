package com.smartcloudbrain.admin.service.storage;

import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyRequest;
import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class AliyunOssStorageService implements ObjectStorageService {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private static final DateTimeFormatter OBJECT_DATE = DateTimeFormatter.ofPattern("yyyy/MM");

  private final ObjectStorageProperties properties;

  public AliyunOssStorageService(ObjectStorageProperties properties) {
    this.properties = properties;
  }

  @Override
  public AssetUploadPolicyResponse createUploadPolicy(AssetUploadPolicyRequest request) {
    validateStorageConfig();
    ValidatedAsset asset = validateAsset(request);
    Instant expiresAt = Instant.now().plusSeconds(Math.max(60, properties.getPolicyExpiresSeconds()));
    String objectKey = objectKey(asset.extension());
    String policy = Base64.getEncoder().encodeToString(policyJson(objectKey, asset.contentType(), expiresAt).getBytes(StandardCharsets.UTF_8));
    String signature = signature(policy);
    Map<String, String> formData = new LinkedHashMap<>();
    formData.put("key", objectKey);
    formData.put("policy", policy);
    formData.put("OSSAccessKeyId", properties.getAccessKeyId().trim());
    formData.put("Signature", signature);
    formData.put("Content-Type", asset.contentType());
    formData.put("success_action_status", "201");
    return new AssetUploadPolicyResponse(
        "aliyun-oss",
        properties.getBucket().trim(),
        objectKey,
        "POST",
        uploadUrl(),
        Map.of(),
        formData,
        publicUrl(objectKey),
        expiresAt.toEpochMilli()
    );
  }

  private ValidatedAsset validateAsset(AssetUploadPolicyRequest request) {
    if (!"patient-site".equals(text(request.scene()))) {
      throw new BusinessException(400, "Unsupported upload scene: " + text(request.scene()));
    }
    if (request.size() <= 0) {
      throw new BusinessException(400, "File size must be greater than 0");
    }
    if (request.size() > properties.getMaxSizeBytes()) {
      throw new BusinessException(400, "File size exceeds object storage limit");
    }
    String contentType = text(request.contentType()).toLowerCase(Locale.ROOT);
    if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BusinessException(400, "Unsupported image content type");
    }
    String extension = extensionOf(request.fileName());
    if (!matchesContentType(extension, contentType)) {
      throw new BusinessException(400, "File extension does not match image content type");
    }
    return new ValidatedAsset(contentType, extension);
  }

  private String policyJson(String objectKey, String contentType, Instant expiresAt) {
    return """
        {"expiration":"%s","conditions":[{"bucket":"%s"},{"key":"%s"},["content-length-range",1,%d],["eq","$Content-Type","%s"]]}
        """.formatted(
            expiresAt.toString(),
            json(properties.getBucket().trim()),
            json(objectKey),
            properties.getMaxSizeBytes(),
            json(contentType)
        ).trim();
  }

  private String signature(String policy) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(properties.getAccessKeySecret().trim().getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
      return Base64.getEncoder().encodeToString(mac.doFinal(policy.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new BusinessException(500, "Object storage policy signing failed");
    }
  }

  private String objectKey(String extension) {
    LocalDate now = LocalDate.now(ZoneId.of("Asia/Shanghai"));
    String prefix = trimSlashes(text(properties.getUploadPrefix()));
    return "%s/%s/%s.%s".formatted(prefix.isBlank() ? "patient-site" : prefix, OBJECT_DATE.format(now), UUID.randomUUID(), extension);
  }

  private String uploadUrl() {
    String endpoint = stripTrailingSlash(properties.getEndpoint().trim());
    if (properties.isForcePathStyle()) {
      return endpoint + "/" + properties.getBucket().trim();
    }
    String scheme = "";
    String host = endpoint;
    int schemeIndex = endpoint.indexOf("://");
    if (schemeIndex > 0) {
      scheme = endpoint.substring(0, schemeIndex + 3);
      host = endpoint.substring(schemeIndex + 3);
    }
    return scheme + properties.getBucket().trim() + "." + host;
  }

  private String publicUrl(String objectKey) {
    String base = text(properties.getPublicBaseUrl());
    if (base.isBlank()) {
      base = uploadUrl();
    }
    return stripTrailingSlash(base) + "/" + objectKey;
  }

  private void validateStorageConfig() {
    if (!hasText(properties.getEndpoint()) || !hasText(properties.getBucket())
        || !hasText(properties.getAccessKeyId()) || !hasText(properties.getAccessKeySecret())) {
      throw new BusinessException(500, "Object storage is not configured");
    }
  }

  private boolean matchesContentType(String extension, String contentType) {
    return switch (extension) {
      case "jpg", "jpeg" -> "image/jpeg".equals(contentType);
      case "png" -> "image/png".equals(contentType);
      case "webp" -> "image/webp".equals(contentType);
      default -> false;
    };
  }

  private String extensionOf(String fileName) {
    String value = text(fileName).toLowerCase(Locale.ROOT);
    int dot = value.lastIndexOf('.');
    if (dot < 0 || dot == value.length() - 1) {
      throw new BusinessException(400, "Image file extension is required");
    }
    return value.substring(dot + 1);
  }

  private String json(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean hasText(String value) {
    return !text(value).isBlank();
  }

  private String trimSlashes(String value) {
    return value.replaceAll("^/+", "").replaceAll("/+$", "");
  }

  private String stripTrailingSlash(String value) {
    return value.replaceAll("/+$", "");
  }

  private record ValidatedAsset(String contentType, String extension) {}
}
