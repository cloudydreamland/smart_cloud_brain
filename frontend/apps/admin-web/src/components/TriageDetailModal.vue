<script setup lang="ts">
import { computed } from "vue";
import { Modal } from "@smart-cloud-brain/shared-ui";
import { displayText, type TriageRecord } from "@smart-cloud-brain/shared-api";

const props = defineProps<{ open: boolean; triage: TriageRecord | null }>();
defineEmits<{ close: [] }>();

const complaintLines = computed(() => {
  const raw = displayText(props.triage?.chiefComplaint, "");
  if (!raw) return [];
  if (!raw.includes("；")) return [{ label: "", value: raw }];
  return raw
    .split("；")
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => {
      const idx = s.indexOf("：");
      if (idx > 0 && idx < 10) {
        return { label: s.slice(0, idx + 1), value: s.slice(idx + 1) };
      }
      return { label: "", value: s };
    });
});
</script>

<template>
  <Modal :open="open" title="分诊详情" @close="$emit('close')">
    <div v-if="triage" class="clinical-note">
      <strong>#{{ displayText(triage.triageRecordId) }} {{ displayText(triage.recommendedDepartment) }}</strong>
      <div class="complaint-lines">
        <p v-for="(line, i) in complaintLines" :key="i">
          <strong v-if="line.label">{{ line.label }}</strong>{{ line.value }}
        </p>
      </div>
      <p v-if="triage.reason" class="ai-reason">
        <strong>AI建议：</strong>{{ triage.reason }}
      </p>
    </div>
  </Modal>
</template>

<style scoped>
.complaint-lines p {
  margin: 2px 0;
}
.complaint-lines p strong {
  font-weight: 600;
}
.ai-reason {
  margin-top: 8px;
}
.ai-reason strong {
  font-weight: 600;
}
</style>
