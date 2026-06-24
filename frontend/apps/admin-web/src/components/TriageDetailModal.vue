<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { displayText, type TriageRecord } from "@smart-cloud-brain/shared-api";
defineProps<{ open: boolean; triage: TriageRecord | null }>();
defineEmits<{ close: [] }>();
</script>
<template><Modal :open="open" title="分诊详情" @close="$emit('close')"><div v-if="triage" class="clinical-note"><strong>#{{ displayText(triage.triageRecordId) }} {{ displayText(triage.recommendedDepartment) }}</strong><p>{{ displayText(triage.chiefComplaint) }}</p><p>{{ displayText(triage.reason) }}</p></div></Modal></template>
