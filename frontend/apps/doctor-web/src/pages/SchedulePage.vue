<script setup lang="ts">
import { ref } from "vue";
import { api, displayText, formatApiError, useAuthStore, usePagination, type Schedule } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const rows = ref<Schedule[]>([]);
const loading = ref(false);
const error = ref("");
const startDate = ref(new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10));
const endDate = ref(new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10));
const status = ref("");
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.doctorSchedules(auth.token(), { startDate: startDate.value, endDate: endDate.value, status: status.value }) as Schedule[];
  } catch (err) {
    error.value = formatApiError(err, "Doctor schedule failed");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="clinical-page">
    <section class="panel">
      <header>
        <div class="panel-title"><p class="eyebrow">Schedule</p><h2>My Published Schedules</h2></div>
        <div class="toolbar"><input v-model="startDate" type="date" /><input v-model="endDate" type="date" /><select v-model="status"><option value="">All</option><option value="PUBLISHED">Published</option><option value="CANCELLED">Cancelled</option></select><button type="button" @click="refresh">Refresh</button></div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="loading" />
        <div v-if="rows.length" class="table-wrap table-breakout">
          <table class="data-table">
            <thead><tr><th>Date</th><th>Time</th><th>Department</th><th>Capacity</th><th>Booked</th><th>Remaining</th><th>Status</th></tr></thead>
            <tbody>
              <tr v-for="item in pageRows" :key="String(item.id)">
                <td>{{ displayText(item.workDate) }}</td>
                <td>{{ displayText(item.timeRange) }}</td>
                <td>{{ displayText(item.departmentName) }}</td>
                <td>{{ displayText(item.capacity) }}</td>
                <td>{{ displayText(item.booked, "0") }}</td>
                <td>{{ displayText(item.remainingCapacity, "0") }}</td>
                <td><StatusTag :status="displayText(item.status)" /></td>
              </tr>
            </tbody>
          </table>
          <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
        </div>
        <EmptyState v-else title="No schedules" />
      </div>
    </section>
  </section>
</template>
