package com.smartcloudbrain.admin.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 患者端配置历史分页响应。
 * 使用明确 DTO 替代裸 Map，避免 Jackson 序列化 Result&lt;Map&gt; 时展平 values。
 */
public record PatientSiteConfigHistoryResponse(
    @JsonProperty("items") List<?> items,
    @JsonProperty("page") int page,
    @JsonProperty("pageSize") int pageSize,
    @JsonProperty("total") long total,
    @JsonProperty("totalPages") int totalPages
) {}
