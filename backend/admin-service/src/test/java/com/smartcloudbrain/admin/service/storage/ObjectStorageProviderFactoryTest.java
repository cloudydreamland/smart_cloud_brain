package com.smartcloudbrain.admin.service.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smartcloudbrain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ObjectStorageProviderFactoryTest {

  @Test
  void rejectsUnsupportedProvider() {
    ObjectStorageProperties properties = new ObjectStorageProperties();
    properties.setProvider("aws-s3");
    ObjectStorageProviderFactory factory = new ObjectStorageProviderFactory(properties, new AliyunOssStorageService(properties));

    assertThrows(BusinessException.class, factory::currentProvider);
  }
}
