package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.admin.client.InternalAiClient;
import com.smartcloudbrain.admin.client.InternalDoctorClient;
import com.smartcloudbrain.admin.client.InternalAiClient;
import com.smartcloudbrain.admin.client.InternalTriageClient;
import com.smartcloudbrain.admin.config.AdminRedisCacheConfig;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.dto.admin.SystemDictSaveRequest;
import com.smartcloudbrain.admin.entity.Drug;
import com.smartcloudbrain.admin.entity.KnowledgeEntry;
import com.smartcloudbrain.admin.entity.SystemDict;
import com.smartcloudbrain.admin.repository.AiScheduleSuggestionRepository;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.DrugRepository;
import com.smartcloudbrain.admin.repository.KnowledgeEntryRepository;
import com.smartcloudbrain.admin.repository.PromptTemplateRepository;
import com.smartcloudbrain.admin.repository.SystemDictRepository;
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
  @Autowired private KnowledgeEntryRepository knowledgeEntryRepository;
  @Autowired private SystemDictRepository systemDictRepository;
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void resetMocksAndCache() {
    reset(drugRepository, knowledgeEntryRepository, systemDictRepository);
    Cache cache = cacheManager.getCache("adminCatalog");
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  void repeatedCatalogReadsUseCache() {
    when(drugRepository.findAll()).thenReturn(List.of(drug("aspirin")));
    when(knowledgeEntryRepository.findByStatus("ENABLED")).thenReturn(List.of(knowledge("cold")));
    when(systemDictRepository.findAll()).thenReturn(List.of(dict("TRIAGE_STATUS", "CREATED")));

    adminCatalogService.drugs();
    adminCatalogService.drugs();
    adminCatalogService.searchKnowledge("cold", "");
    adminCatalogService.searchKnowledge("cold", "");
    adminCatalogService.dicts(null);
    adminCatalogService.dicts(null);

    verify(drugRepository, times(1)).findAll();
    verify(knowledgeEntryRepository, times(1)).findByStatus("ENABLED");
    verify(systemDictRepository, times(1)).findAll();
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
  void dictWritesEvictCatalogCache() {
    when(systemDictRepository.findAll()).thenReturn(List.of(dict("TRIAGE_STATUS", "CREATED")));
    when(systemDictRepository.save(any(SystemDict.class))).thenAnswer(invocation -> {
      SystemDict dict = invocation.getArgument(0);
      dict.setId(11L);
      return dict;
    });

    adminCatalogService.dicts(null);
    adminCatalogService.saveDict(new SystemDictSaveRequest(null, "TRIAGE_STATUS", "CLOSED", "closed", 2, "ENABLED"));
    adminCatalogService.dicts(null);

    verify(systemDictRepository, times(2)).findAll();
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

  private static KnowledgeEntry knowledge(String title) {
    KnowledgeEntry entry = new KnowledgeEntry();
    entry.setId(2L);
    entry.setTitle(title);
    entry.setSymptoms(title);
    entry.setRiskSignals("");
    entry.setAdvice("rest");
    entry.setDepartmentCode("GENERAL");
    entry.setStatus("ENABLED");
    return entry;
  }

  private static SystemDict dict(String type, String key) {
    SystemDict dict = new SystemDict();
    dict.setId(3L);
    dict.setDictType(type);
    dict.setDictKey(key);
    dict.setDictValue(key);
    dict.setSort(1);
    dict.setStatus("ENABLED");
    return dict;
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
        PromptTemplateRepository promptTemplateRepository,
        KnowledgeEntryRepository knowledgeEntryRepository,
        SystemDictRepository systemDictRepository,
        AiScheduleSuggestionRepository aiScheduleSuggestionRepository,
        InternalDoctorClient internalDoctorClient,
        InternalAiClient internalAiClient,
        InternalTriageClient internalTriageClient,
        InternalAiClient internalAiClient,
        PasswordHashService passwordHashService
    ) {
      return new AdminCatalogService(
          departmentRepository,
          doctorRepository,
          drugRepository,
          promptTemplateRepository,
          knowledgeEntryRepository,
          systemDictRepository,
          aiScheduleSuggestionRepository,
          internalDoctorClient,
          internalAiClient,
          internalTriageClient,
          internalAiClient,
          passwordHashService,
          new ObjectMapper()
      );
    }

    @Bean DepartmentRepository departmentRepository() { return mock(DepartmentRepository.class); }
    @Bean DoctorRepository doctorRepository() { return mock(DoctorRepository.class); }
    @Bean DrugRepository drugRepository() { return mock(DrugRepository.class); }
    @Bean PromptTemplateRepository promptTemplateRepository() { return mock(PromptTemplateRepository.class); }
    @Bean KnowledgeEntryRepository knowledgeEntryRepository() { return mock(KnowledgeEntryRepository.class); }
    @Bean SystemDictRepository systemDictRepository() { return mock(SystemDictRepository.class); }
    @Bean AiScheduleSuggestionRepository aiScheduleSuggestionRepository() { return mock(AiScheduleSuggestionRepository.class); }
    @Bean InternalDoctorClient internalDoctorClient() { return mock(InternalDoctorClient.class); }
    @Bean InternalAiClient internalAiClient() { return mock(InternalAiClient.class); }
    @Bean InternalTriageClient internalTriageClient() { return mock(InternalTriageClient.class); }
    @Bean InternalAiClient internalAiClient() { return mock(InternalAiClient.class); }
    @Bean PasswordHashService passwordHashService() { return mock(PasswordHashService.class); }
  }
}
