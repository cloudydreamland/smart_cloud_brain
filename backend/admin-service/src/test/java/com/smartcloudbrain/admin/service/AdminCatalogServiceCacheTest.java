package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.client.InternalAiClient;
import com.smartcloudbrain.admin.client.InternalDoctorClient;
import com.smartcloudbrain.admin.client.InternalPatientCacheClient;
import com.smartcloudbrain.admin.client.InternalTriageClient;
import com.smartcloudbrain.admin.config.AdminRedisCacheConfig;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.entity.Drug;
import com.smartcloudbrain.admin.repository.AiScheduleSuggestionRepository;
import com.smartcloudbrain.admin.repository.AdminUserRepository;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.DrugRepository;
import com.smartcloudbrain.common.security.PasswordHashService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AdminCatalogServiceCacheTest.CacheTestConfig.class)
class AdminCatalogServiceCacheTest {

  @Autowired private AdminCatalogService adminCatalogService;
  @Autowired private DrugRepository drugRepository;
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void resetMocksAndCache() {
    reset(drugRepository);
    Cache cache = cacheManager.getCache("adminCatalog");
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  void repeatedCatalogReadsUseCache() {
    when(drugRepository.findAll()).thenReturn(List.of(drug("aspirin")));

    adminCatalogService.drugs();
    adminCatalogService.drugs();

    verify(drugRepository, times(1)).findAll();
  }

  @Test
  void writesEvictCatalogCache() {
    when(drugRepository.findAll()).thenReturn(List.of(drug("aspirin")));
    when(drugRepository.save(any(Drug.class))).thenAnswer(invocation -> {
      Drug drug = invocation.getArgument(0);
      drug.setId(10L);
      return drug;
    });

    adminCatalogService.drugs();
    adminCatalogService.saveDrug(new DrugSaveRequest(null, "ibuprofen", "0.2g", "", "", "ENABLED"));
    adminCatalogService.drugs();

    verify(drugRepository, times(2)).findAll();
  }

  @Test
  void cacheErrorHandlerDoesNotBreakBusinessFallback() {
    CacheErrorHandler handler = new AdminRedisCacheConfig().errorHandler();
    Cache cache = mock(Cache.class);
    when(cache.getName()).thenReturn("adminCatalog");

    assertDoesNotThrow(() -> handler.handleCacheGetError(new RuntimeException("redis down"), cache, "drugs"));
    assertDoesNotThrow(() -> handler.handleCachePutError(new RuntimeException("redis down"), cache, "drugs", List.of()));
    assertDoesNotThrow(() -> handler.handleCacheEvictError(new RuntimeException("redis down"), cache, "drugs"));
    assertDoesNotThrow(() -> handler.handleCacheClearError(new RuntimeException("redis down"), cache));
  }

  private static Drug drug(String name) {
    Drug drug = new Drug();
    drug.setId(1L);
    drug.setName(name);
    drug.setSpecification("0.1g");
    drug.setContraindication("");
    drug.setInteractionRule("");
    drug.setStatus("ENABLED");
    return drug;
  }

  @Configuration
  @EnableCaching
  static class CacheTestConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("adminCatalog");
    }

    @Bean
    AdminCatalogService adminCatalogService(
        DepartmentRepository departmentRepository,
        DoctorRepository doctorRepository,
        DrugRepository drugRepository,
        AiScheduleSuggestionRepository aiScheduleSuggestionRepository,
        AdminUserRepository adminUserRepository,
        InternalDoctorClient internalDoctorClient,
        InternalPatientCacheClient internalPatientCacheClient,
        InternalAiClient internalAiClient,
        InternalTriageClient internalTriageClient,
        PasswordHashService passwordHashService
    ) {
      return new AdminCatalogService(
          departmentRepository,
          doctorRepository,
          drugRepository,
          aiScheduleSuggestionRepository,
          adminUserRepository,
          internalDoctorClient,
          internalPatientCacheClient,
          internalAiClient,
          internalTriageClient,
          passwordHashService
      );
    }

    @Bean DepartmentRepository departmentRepository() { return mock(DepartmentRepository.class); }
    @Bean DoctorRepository doctorRepository() { return mock(DoctorRepository.class); }
    @Bean DrugRepository drugRepository() { return mock(DrugRepository.class); }
    @Bean AiScheduleSuggestionRepository aiScheduleSuggestionRepository() { return mock(AiScheduleSuggestionRepository.class); }
    @Bean AdminUserRepository adminUserRepository() { return mock(AdminUserRepository.class); }
    @Bean InternalDoctorClient internalDoctorClient() { return mock(InternalDoctorClient.class); }
    @Bean InternalPatientCacheClient internalPatientCacheClient() { return mock(InternalPatientCacheClient.class); }
    @Bean InternalAiClient internalAiClient() { return mock(InternalAiClient.class); }
    @Bean InternalTriageClient internalTriageClient() { return mock(InternalTriageClient.class); }
    @Bean PasswordHashService passwordHashService() { return mock(PasswordHashService.class); }
  }
}
