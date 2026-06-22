<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusText, toNumber, useAuthStore, usePatientWorkflowStore, type PatientSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, LoadingState } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { patient } = storeToRefs(workflow);
const loading = ref(false);
const error = ref("");
const notice = ref("");
const form = reactive<PatientSaveRequest>({ name: "", gender: "", age: 0, allergyHistory: "", pastHistory: "" });

watch(patient, (value) => {
  form.name = fieldText(value, "name", auth.session?.name || "");
  form.gender = fieldText(value, "gender", "");
  form.age = toNumber(value?.age, 0);
  form.allergyHistory = fieldText(value, "allergyHistory", "");
  form.pastHistory = fieldText(value, "pastHistory", "");
}, { immediate: true });

async function save() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    patient.value = await api.saveProfile(auth.token(), { ...form });
    await workflow.refreshAuthenticated(auth.token());
    notice.value = "Profile saved";
  } catch (err) {
    error.value = formatApiError(err, "Profile save failed");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="portal-grid">
    <section class="panel">
      <header class="panel-header"><div class="panel-title"><p class="eyebrow">Profile</p><h2>Patient Profile</h2><p>Update clinical profile fields used by triage and prescription checks.</p></div></header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="loading" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <template v-if="patient">
          <div class="form-grid">
            <FormField label="Name"><input v-model.trim="form.name" /></FormField>
            <FormField label="Gender"><select v-model="form.gender"><option value="">Unknown</option><option value="MALE">Male</option><option value="FEMALE">Female</option></select></FormField>
            <FormField label="Age"><input v-model.number="form.age" type="number" min="0" max="130" /></FormField>
          </div>
          <FormField label="Allergy history"><textarea v-model.trim="form.allergyHistory" /></FormField>
          <FormField label="Past history"><textarea v-model.trim="form.pastHistory" /></FormField>
          <button class="primary" type="button" :disabled="loading" @click="save">Save profile</button>
        </template>
        <EmptyState v-else title="No patient profile" message="Refresh or sign in again." />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>Account</h2><p>Login state protects patient portal pages.</p></div></header>
      <div class="panel-body">
        <div class="summary-strip">
          <div class="summary-item"><span>Role</span><strong>{{ statusText(auth.session?.role, "-") }}</strong></div>
          <div class="summary-item"><span>User ID</span><strong>{{ auth.session?.userId }}</strong></div>
          <div class="summary-item"><span>Phone</span><strong>{{ fieldText(patient, "phone", "-") }}</strong></div>
        </div>
      </div>
    </aside>
  </section>
</template>
