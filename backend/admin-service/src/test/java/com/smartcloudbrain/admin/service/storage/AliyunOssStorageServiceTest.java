package com.smartcloudbrain.admin.service.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyRequest;
import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AliyunOssStorageServiceTest {

  @Test
  void createsPostPolicyForValidImage() {
    AliyunOssStorageService service = new AliyunOssStorageService(properties());

    AssetUploadPolicyResponse response = service.createUploadPolicy(new AssetUploadPolicyRequest(
        "patient-site", "logo.png", "image/png", 1024
    ));

    assertEquals("aliyun-oss", response.provider());
    assertEquals("bucket-a", response.bucket());
    assertEquals("POST", response.uploadMethod());
    assertTrue(response.uploadUrl().startsWith("https://bucket-a."));
    assertTrue(response.objectKey().startsWith("patient-site/"));
    assertTrue(response.objectKey().endsWith(".png"));
    assertEquals("https://cdn.example.com/" + response.objectKey(), response.publicUrl());
    assertEquals(response.objectKey(), response.formData().get("key"));
    assertEquals("image/png", response.formData().get("Content-Type"));
    String policy = new String(Base64.getDecoder().decode(response.formData().get("policy")), StandardCharsets.UTF_8);
    assertTrue(policy.contains("\"bucket\":\"bucket-a\""));
    assertTrue(policy.contains("\"key\":\"" + response.objectKey() + "\""));
    assertTrue(policy.contains("\"content-length-range\",1,5242880"));
    assertTrue(policy.contains("\"$Content-Type\",\"image/png\""));
    assertFalse(response.formData().containsValue("secret-a"));
  }

  @Test
  void rejectsUnsupportedSceneMimeSizeAndExtensionMismatch() {
    AliyunOssStorageService service = new AliyunOssStorageService(properties());

    assertThrows(BusinessException.class, () -> service.createUploadPolicy(new AssetUploadPolicyRequest(
        "other", "logo.png", "image/png", 1024
    )));
    assertThrows(BusinessException.class, () -> service.createUploadPolicy(new AssetUploadPolicyRequest(
        "patient-site", "logo.svg", "image/svg+xml", 1024
    )));
    assertThrows(BusinessException.class, () -> service.createUploadPolicy(new AssetUploadPolicyRequest(
        "patient-site", "logo.png", "image/png", 6 * 1024 * 1024
    )));
    assertThrows(BusinessException.class, () -> service.createUploadPolicy(new AssetUploadPolicyRequest(
        "patient-site", "logo.jpg", "image/png", 1024
    )));
  }

  @Test
  void reportsMissingStorageConfigurationWithoutSecrets() {
    ObjectStorageProperties properties = properties();
    properties.setAccessKeySecret("");
    AliyunOssStorageService service = new AliyunOssStorageService(properties);

    BusinessException error = assertThrows(BusinessException.class, () -> service.createUploadPolicy(new AssetUploadPolicyRequest(
        "patient-site", "logo.png", "image/png", 1024
    )));

    assertEquals(500, error.code());
    assertFalse(error.getMessage().contains("secret"));
  }

  private ObjectStorageProperties properties() {
    ObjectStorageProperties properties = new ObjectStorageProperties();
    properties.setProvider("aliyun-oss");
    properties.setEndpoint("https://oss-cn-example.aliyuncs.com");
    properties.setBucket("bucket-a");
    properties.setPublicBaseUrl("https://cdn.example.com");
    properties.setAccessKeyId("key-a");
    properties.setAccessKeySecret("secret-a");
    properties.setUploadPrefix("patient-site");
    properties.setMaxSizeBytes(5 * 1024 * 1024);
    properties.setPolicyExpiresSeconds(600);
    return properties;
  }
}
