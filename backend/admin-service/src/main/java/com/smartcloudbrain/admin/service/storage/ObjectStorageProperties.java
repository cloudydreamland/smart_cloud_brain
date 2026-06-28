package com.smartcloudbrain.admin.service.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scb.object-storage")
public class ObjectStorageProperties {

  private String provider = "aliyun-oss";
  private String endpoint = "";
  private String region = "";
  private String bucket = "";
  private String publicBaseUrl = "";
  private String accessKeyId = "";
  private String accessKeySecret = "";
  private String uploadPrefix = "patient-site";
  private long maxSizeBytes = 5 * 1024 * 1024;
  private long policyExpiresSeconds = 600;
  private boolean forcePathStyle = false;

  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }
  public String getEndpoint() { return endpoint; }
  public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
  public String getRegion() { return region; }
  public void setRegion(String region) { this.region = region; }
  public String getBucket() { return bucket; }
  public void setBucket(String bucket) { this.bucket = bucket; }
  public String getPublicBaseUrl() { return publicBaseUrl; }
  public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
  public String getAccessKeyId() { return accessKeyId; }
  public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
  public String getAccessKeySecret() { return accessKeySecret; }
  public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
  public String getUploadPrefix() { return uploadPrefix; }
  public void setUploadPrefix(String uploadPrefix) { this.uploadPrefix = uploadPrefix; }
  public long getMaxSizeBytes() { return maxSizeBytes; }
  public void setMaxSizeBytes(long maxSizeBytes) { this.maxSizeBytes = maxSizeBytes; }
  public long getPolicyExpiresSeconds() { return policyExpiresSeconds; }
  public void setPolicyExpiresSeconds(long policyExpiresSeconds) { this.policyExpiresSeconds = policyExpiresSeconds; }
  public boolean isForcePathStyle() { return forcePathStyle; }
  public void setForcePathStyle(boolean forcePathStyle) { this.forcePathStyle = forcePathStyle; }
}
