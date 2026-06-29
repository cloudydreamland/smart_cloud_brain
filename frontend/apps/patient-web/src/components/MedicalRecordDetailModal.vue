<script setup lang="ts">
import { computed } from "vue";
import { Modal, EmptyState } from "@smart-cloud-brain/shared-ui";
import { fieldText, type DataRow } from "@smart-cloud-brain/shared-api";

const props = defineProps<{ open: boolean; record: DataRow | null }>();
defineEmits<{ close: [] }>();

const formattedTime = computed(() => {
  const raw = fieldText(props.record, "createdAt", "");
  if (!raw) return "未同步";
  try {
    const d = new Date(raw);
    if (isNaN(d.getTime())) return raw;
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return raw;
  }
});
</script>

<template>
  <Modal
    :open="open"
    title="病历详情"
    description="以下内容以医生最终保存记录为准。"
    size="lg"
    @close="$emit('close')"
  >
    <div v-if="record" class="detail-modal-content">
      <div class="detail-section">
        <h4>基本信息</h4>
        <div class="detail-row">
          <span>病历编号</span>
          <strong>#{{ fieldText(record, "medicalRecordId", fieldText(record, "id")) }}</strong>
        </div>
        <div class="detail-row">
          <span>保存时间</span>
          <strong>{{ formattedTime }}</strong>
        </div>
      </div>

      <div class="detail-section">
        <h4>诊断信息</h4>
        <div class="detail-row">
          <span>诊断</span>
          <strong>{{ fieldText(record, "diagnosis", "未记录") }}</strong>
        </div>
        <div class="detail-row">
          <span>主诉</span>
          <strong>{{ fieldText(record, "chiefComplaint", "未记录") }}</strong>
        </div>
      </div>

      <div class="detail-section">
        <h4>病史与检查</h4>
        <div class="detail-row">
          <span>现病史</span>
          <strong>{{ fieldText(record, "presentIllness", "未记录") }}</strong>
        </div>
        <div class="detail-row">
          <span>既往史</span>
          <strong>{{ fieldText(record, "pastHistory", "未记录") }}</strong>
        </div>
        <div class="detail-row">
          <span>体格检查</span>
          <strong>{{ fieldText(record, "physicalExam", "未记录") }}</strong>
        </div>
      </div>

      <div class="detail-section">
        <h4>处理建议</h4>
        <p>{{ fieldText(record, "treatmentAdvice", "未记录") }}</p>
      </div>
    </div>
    <EmptyState v-else title="未选择病历" message="请选择一条病历查看详情。" />
  </Modal>
</template>
