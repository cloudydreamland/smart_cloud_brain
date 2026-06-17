<script setup lang="ts">
import { Modal, EmptyState } from "@smart-cloud-brain/shared-ui";
import { fieldText, type DataRow } from "@smart-cloud-brain/shared-api";

defineProps<{ open: boolean; record: DataRow | null }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Modal :open="open" title="病历详情" description="以下内容以医生最终保存记录为准。" @close="$emit('close')">
    <dl v-if="record" class="detail-list">
      <div><dt>诊断</dt><dd>{{ fieldText(record, "diagnosis") }}</dd></div>
      <div><dt>主诉</dt><dd>{{ fieldText(record, "chiefComplaint") }}</dd></div>
      <div><dt>现病史</dt><dd>{{ fieldText(record, "presentIllness", "未记录") }}</dd></div>
      <div><dt>既往史</dt><dd>{{ fieldText(record, "pastHistory", "未记录") }}</dd></div>
      <div><dt>处理建议</dt><dd>{{ fieldText(record, "treatmentAdvice", "未记录") }}</dd></div>
    </dl>
    <EmptyState v-else title="未选择病历" message="请选择一条病历查看详情。" />
  </Modal>
</template>
