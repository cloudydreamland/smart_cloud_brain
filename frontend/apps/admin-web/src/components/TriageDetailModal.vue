<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { fieldText, type DataRow } from "@smart-cloud-brain/shared-api";
defineProps<{ open: boolean; triage: DataRow | null }>();
defineEmits<{ close: [] }>();
</script>
<template><Modal :open="open" title="分诊详情" @close="$emit('close')"><div v-if="triage" class="clinical-note"><strong>#{{ fieldText(triage, "triageRecordId") }} {{ fieldText(triage, "recommendedDepartment") }}</strong><p>{{ fieldText(triage, "chiefComplaint") }}</p><p>{{ fieldText(triage, "reason") }}</p></div></Modal></template>
