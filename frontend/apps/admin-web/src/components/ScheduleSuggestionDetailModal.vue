<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { aiSourceLabel, aiSourceTone, fieldText, type DataRow } from "@smart-cloud-brain/shared-api";
defineProps<{ open: boolean; suggestion: DataRow | null }>();
defineEmits<{ close: [] }>();
</script>
<template><Modal :open="open" title="排班建议详情" @close="$emit('close')"><div v-if="suggestion" class="clinical-note"><strong>{{ fieldText(suggestion, "doctorName") }} / {{ fieldText(suggestion, "departmentName") }}</strong><p><span class="tag" :class="aiSourceTone(suggestion.source)">{{ aiSourceLabel(suggestion.source) }}</span> · {{ fieldText(suggestion, "source", "unknown") }}</p><p>{{ fieldText(suggestion, "reason", "暂无原因说明") }}</p></div></Modal></template>
