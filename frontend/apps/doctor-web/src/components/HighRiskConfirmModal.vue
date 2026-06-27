<script setup lang="ts">
import { ref } from "vue";
import { Modal } from "@smart-cloud-brain/shared-ui";

defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();

const reviewed = ref(false);
</script>

<template>
  <Modal :open="open" title="高风险处方确认" @close="$emit('close')">
    <div class="high-risk-dialog">
      <div class="high-risk-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      </div>
      <div class="high-risk-body">
        <p><strong>审核结果提示存在高风险</strong></p>
        <p>系统检测到该处方存在以下潜在风险：</p>
        <ul>
          <li>药物与患者过敏史可能存在冲突</li>
          <li>剂量或频次需要人工确认</li>
          <li>药物相互作用需复核</li>
        </ul>
      </div>
      <label class="review-check">
        <input type="checkbox" v-model="reviewed" />
        <span>我已人工复核用药风险，确认继续创建处方</span>
      </label>
    </div>
    <template #footer>
      <button type="button" :disabled="busy" @click="$emit('close')">取消</button>
      <button type="button" class="danger" :disabled="busy || !reviewed" @click="$emit('confirm')">{{ busy ? "处理中..." : "已复核，继续" }}</button>
    </template>
  </Modal>
</template>

<style scoped>
.high-risk-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.high-risk-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fef2f2;
  color: #dc2626;
  margin: 0 auto;
}
.high-risk-body p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--text, #1f2937);
}
.high-risk-body ul {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 0.85rem;
  color: var(--text-secondary, #6b7280);
  line-height: 1.6;
}
.review-check {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.85rem;
  color: #991b1b;
  cursor: pointer;
}
.review-check input[type="checkbox"] {
  accent-color: #dc2626;
  width: 16px;
  height: 16px;
}
</style>
