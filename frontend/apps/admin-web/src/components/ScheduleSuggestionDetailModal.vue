<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { aiSourceLabel, aiSourceTone, displayText, type ScheduleSuggestion } from "@smart-cloud-brain/shared-api";
defineProps<{ open: boolean; suggestion: ScheduleSuggestion | null }>();
defineEmits<{ close: [] }>();
</script>
<template><Modal :open="open" title="排班建议详情" @close="$emit('close')"><div v-if="suggestion" class="clinical-note"><strong>{{ displayText(suggestion.doctorName) }} / {{ displayText(suggestion.departmentName) }}</strong><p><span class="tag" :class="aiSourceTone(suggestion.source)">{{ aiSourceLabel(suggestion.source) }}</span> · {{ displayText(suggestion.source, "unknown") }}</p><p>{{ displayText(suggestion.reason, "暂无原因说明") }}</p></div></Modal></template>
