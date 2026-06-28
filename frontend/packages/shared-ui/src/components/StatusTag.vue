<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{ status?: string | number | null; tone?: string }>();

const label = computed(() => {
  const raw = String(props.status ?? "").trim();
  if (!raw) return "-";
  const labels: Record<string, string> = {
    CREATED: "待接诊",
    CONFIRMED: "已确认",
    COMPLETED: "已完成",
    AVAILABLE: "可预约",
    ENABLED: "启用",
    PUBLISHED: "已发布",
    AI_RECOMMENDED: "智能推荐",
    LOW: "低风险",
    READ: "已读",
    CANCELLED: "已取消",
    FAILED: "失败",
    DISABLED: "停用",
    HIGH: "高风险",
    CLOSED: "已关闭",
    FULL: "已约满",
    PENDING: "待处理",
    DRAFT: "草稿",
    UNPUBLISHED: "未发布",
    UNREVIEWED: "未审核",
    MEDIUM: "中风险",
    MANUAL_REQUIRED: "待人工处理",
    UNREAD: "未读",
    IDLE: "待生成",
    GENERATING: "生成中",
    DRAFT_READY: "草稿已生成",
    IN_USE: "使用中",
    MAINTENANCE: "维修中",
    RETIRED: "已停用",
    PATIENT: "患者",
    DOCTOR: "医生",
    ADMIN: "管理员",
    INFO: "信息",
  };
  return labels[raw.toUpperCase()] ?? raw;
});
</script>

<template>
  <span class="tag" :class="tone">{{ label }}</span>
</template>
