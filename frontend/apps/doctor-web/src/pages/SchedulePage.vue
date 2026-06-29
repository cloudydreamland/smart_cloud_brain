<script setup lang="ts">
import { inject, ref } from "vue";
import { api, displayText, formatApiError, usePagination, type Schedule } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";

const rows = ref<Schedule[]>([]);
const loading = ref(false);
const loaded = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const startDate = ref(new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10));
const endDate = ref(new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10));
const status = ref("");
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.doctorSchedules({ startDate: startDate.value, endDate: endDate.value, status: status.value }) as Schedule[];
    loaded.value = true;
    toast?.value?.success("数据已刷新", "排班数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "加载排班失败");
    toast?.value?.error("刷新失败", "请检查网络后重试。");
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
        <div class="toolbar"><input v-model="startDate" type="date" /><input v-model="endDate" type="date" /><select v-model="status"><option value="">全部</option><option value="PUBLISHED">已发布</option><option value="CANCELLED">已取消</option></select><button type="button" :disabled="loading" @click="refresh"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> 刷新</button></div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="!loaded && loading" />
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
