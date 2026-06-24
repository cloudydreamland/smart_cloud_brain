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
    error.value = formatApiError(err, "加载排班失败");
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
        <div class="panel-title"><p class="eyebrow">排班管理</p><h2>我的排班</h2></div>
        <div class="toolbar"><input v-model="startDate" type="date" /><input v-model="endDate" type="date" /><select v-model="status"><option value="">全部</option><option value="PUBLISHED">已发布</option><option value="CANCELLED">已取消</option></select><button type="button" @click="refresh">刷新</button></div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="loading" />
        <div v-if="rows.length" class="table-wrap table-breakout">
          <table class="data-table">
            <thead><tr><th>日期</th><th>时间</th><th>科室</th><th>容量</th><th>已预约</th><th>剩余</th><th>状态</th></tr></thead>
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
        <EmptyState v-else title="暂无排班" />
      </div>
    </section>
  </section>
</template>
