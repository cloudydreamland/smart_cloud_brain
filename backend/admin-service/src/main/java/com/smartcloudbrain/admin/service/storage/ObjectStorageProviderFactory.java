package com.smartcloudbrain.admin.service.storage;

import com.smartcloudbrain.common.exception.BusinessException;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ObjectStorageProviderFactory {

  private final ObjectStorageProperties properties;
  private final AliyunOssStorageService aliyunOssStorageService;

  public ObjectStorageProviderFactory(ObjectStorageProperties properties, AliyunOssStorageService aliyunOssStorageService) {
    this.properties = properties;
    this.aliyunOssStorageService = aliyunOssStorageService;
  }

  public ObjectStorageService currentProvider() {
    String provider = normalize(properties.getProvider());
    if ("aliyun-oss".equals(provider)) {
      return aliyunOssStorageService;
    }
    throw new BusinessException(400, "Unsupported object storage provider: " + provider);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
