<script setup lang="ts">
import { computed } from "vue";
import { Modal } from "@smart-cloud-brain/shared-ui";
import { displayText, toNumber, type Notification } from "@smart-cloud-brain/shared-api";
import { formatTime, statusLabel, statusTone } from "../doctorPresentation";

const props = defineProps<{ open: boolean; notification: Notification | null }>();
defineEmits<{
  close: [];
  read: [];
  handle: [status: "HANDLED" | "IGNORED"];
  process: [];
}>();

const readStatus = computed(() => displayText(props.notification?.readStatus, "UNREAD").toUpperCase());
const handleStatus = computed(() => displayText(props.notification?.handleStatus, readStatus.value === "READ" ? "HANDLED" : "PENDING").toUpperCase());
const riskLevel = computed(() => displayText(props.notification?.riskLevel, "INFO").toUpperCase());
const relationRows = computed(() => {
  const item = props.notification;
  if (!item) return [];
  return [
    ["患者", toNumber(item.patientId) ? `#${item.patientId}` : "-"],
    ["处方", toNumber(item.prescriptionId) ? `#${item.prescriptionId}` : "-"],
    ["分诊", toNumber(item.triageRecordId) ? `#${item.triageRecordId}` : "-"],
    ["病历", toNumber(item.medicalRecordId) ? `#${item.medicalRecordId}` : "-"],
  ];
});

function typeLabel(type: unknown) {
  const value = displayText(type, "SYSTEM_NOTICE").toUpperCase();
  if (value.includes("TRIAGE")) return "分诊通知";
  if (value.includes("PRESCRIPTION")) return "处方通知";
  if (value.includes("MEDICAL_RECORD")) return "病历通知";
  if (value.includes("REGISTRATION")) return "挂号通知";
  return "系统通知";
}
</script>

<template>
  <Modal :open="open" title="通知详情" @close="$emit('close')">
    <div v-if="notification" class="notification-detail">
      <div class="detail-badges">
        <span class="tag active">{{ typeLabel(notification.type) }}</span>
        <span class="tag" :class="statusTone(riskLevel)">{{ statusLabel(riskLevel) }}</span>
        <span class="tag" :class="statusTone(readStatus)">{{ statusLabel(readStatus) }}</span>
        <span class="tag" :class="statusTone(handleStatus)">{{ statusLabel(handleStatus) }}</span>
      </div>
      <h3>{{ displayText(notification.title, "系统通知") }}</h3>
      <p>{{ displayText(notification.content, "暂无通知正文") }}</p>
      <dl>
        <div v-for="[label, value] in relationRows" :key="label">
          <dt>{{ label }}</dt>
          <dd>{{ value }}</dd>
        </div>
        <div>
          <dt>创建时间</dt>
          <dd>{{ formatTime(notification.createdAt) }}</dd>
        </div>
        <div>
          <dt>处理时间</dt>
          <dd>{{ notification.handledAt ? formatTime(notification.handledAt) : "-" }}</dd>
        </div>
      </dl>
    </div>
    <template #footer>
      <button type="button" @click="$emit('close')">关闭</button>
      <button v-if="notification && readStatus !== 'READ'" type="button" @click="$emit('read')">标记已读</button>
      <button v-if="notification && handleStatus === 'PENDING'" type="button" @click="$emit('handle', 'IGNORED')">忽略</button>
      <button v-if="notification" type="button" class="primary" @click="$emit('process')">去处理</button>
      <button v-if="notification && handleStatus === 'PENDING'" type="button" class="primary" @click="$emit('handle', 'HANDLED')">标记已处理</button>
    </template>
  </Modal>
</template>
