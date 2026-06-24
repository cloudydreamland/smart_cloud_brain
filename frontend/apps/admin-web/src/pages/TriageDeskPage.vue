<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusClass, toNumber, useAdminWorkflowStore, useAuthStore, usePagination, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";
import TriageDetailModal from "../components/TriageDetailModal.vue";
import AssignDoctorModal from "../components/AssignDoctorModal.vue";
import CloseTriageConfirmModal from "../components/CloseTriageConfirmModal.vue";

const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { triageDesk, doctors, refreshErrors } = storeToRefs(workflow);
const filter = reactive({ keyword: "", department: "", status: "" });
const assignForm = reactive({ triageRecordId: 0, doctorId: 0 });
const selected = ref<DataRow | null>(null);
const closeTarget = ref<DataRow | null>(null);
const assignOpen = ref(false);
const loading = ref(false);
const error = ref("");
const notice = ref("");

const rows = computed(() => triageDesk.value.filter((item) => {
  const keyword = filter.keyword.trim().toLowerCase();
  const haystack = `${fieldText(item, "chiefComplaint", "")} ${fieldText(item, "reason", "")}`.toLowerCase();
  return (!keyword || haystack.includes(keyword))
    && (!filter.department || fieldText(item, "recommendedDepartment", "").includes(filter.department))
    && (!filter.status || fieldText(item, "status") === filter.status);
}));
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 8);

async function detail(item: DataRow) {
  loading.value = true;
  error.value = "";
  try {
    selected.value = await api.triageDetail(auth.token(), toNumber(item.triageRecordId));
  } catch (err) {
    error.value = formatApiError(err, "分诊详情加载失败");
  } finally {
    loading.value = false;
  }
}

function openAssign(item: DataRow) {
  assignForm.triageRecordId = toNumber(item.triageRecordId);
  assignForm.doctorId = toNumber(item.assignedDoctorId, toNumber(doctors.value[0]?.id));
  assignOpen.value = true;
}

async function assign() {
  if (!assignForm.triageRecordId || !assignForm.doctorId) {
    error.value = "请选择分诊记录和医生。";
    return;
  }
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.assignTriage(auth.token(), { ...assignForm });
    assignOpen.value = false;
    emit("refresh");
    notice.value = "分诊已分配给医生。";
  } catch (err) {
    error.value = formatApiError(err, "分诊分配失败");
  } finally {
    loading.value = false;
  }
}

async function closeTriage() {
  if (!closeTarget.value) return;
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.closeTriage(auth.token(), toNumber(closeTarget.value.triageRecordId));
    closeTarget.value = null;
    emit("refresh");
    notice.value = "分诊记录已关闭。";
  } catch (err) {
    error.value = formatApiError(err, "关闭分诊失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="triage-layout">
    <section class="panel">
      <header class="panel-header"><div class="panel-title"><h2>分诊工作台</h2></div></header>
      <div class="panel-body stack">
        <ErrorState v-if="error || refreshErrors.triageDesk" :message="error || refreshErrors.triageDesk" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="admin-filter-row">
          <input v-model.trim="filter.keyword" placeholder="搜索主诉或原因" />
          <input v-model.trim="filter.department" placeholder="推荐科室" />
          <select v-model="filter.status"><option value="">全部状态</option><option value="MANUAL_REQUIRED">待人工处理</option><option value="AI_RECOMMENDED">智能推荐</option><option value="CLOSED">已关闭</option></select>
        </div>
        <div v-if="rows.length" class="table-scroll">
          <table class="data-table">
            <thead><tr><th>记录</th><th>主诉</th><th>推荐科室</th><th>医生</th><th>状态</th><th class="actions-cell">操作</th></tr></thead>
            <tbody>
              <tr v-for="item in pageRows" :key="String(item.triageRecordId)">
                <td>#{{ fieldText(item, "triageRecordId") }}</td>
                <td>{{ fieldText(item, "chiefComplaint") }}</td>
                <td>{{ fieldText(item, "recommendedDepartment") }}</td>
                <td>{{ fieldText(item, "assignedDoctorName", "未分配") }}</td>
                <td><StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" /></td>
                <td class="toolbar"><button type="button" @click="detail(item)">详情</button><button type="button" @click="openAssign(item)">分配</button><button class="danger" type="button" @click="closeTarget = item">关闭</button></td>
              </tr>
            </tbody>
          </table>
          <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
        </div>
        <EmptyState v-else title="暂无分诊记录" />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>分配信息</h2></div></header>
      <div class="panel-body">
        <div class="summary-strip">
          <div class="summary-item"><span>医生数量</span><strong>{{ doctors.length }}</strong></div>
          <div class="summary-item"><span>筛选结果</span><strong>{{ rows.length }}</strong></div>
          <div class="summary-item"><span>状态</span><strong>人工复核</strong></div>
        </div>
      </div>
    </aside>
    <TriageDetailModal :open="Boolean(selected)" :triage="selected" @close="selected = null" />
    <AssignDoctorModal :open="assignOpen" :busy="loading" @close="assignOpen = false" @confirm="assign">
      <div class="stack">
        <FormField label="分诊记录 ID"><input v-model.number="assignForm.triageRecordId" type="number" /></FormField>
        <FormField label="医生">
          <select v-model.number="assignForm.doctorId">
            <option :value="0">请选择</option>
            <option v-for="doctor in doctors" :key="String(doctor.id)" :value="toNumber(doctor.id)">{{ doctor.name }} / {{ doctor.departmentName }}</option>
          </select>
        </FormField>
      </div>
    </AssignDoctorModal>
    <CloseTriageConfirmModal :open="Boolean(closeTarget)" :busy="loading" @close="closeTarget = null" @confirm="closeTriage" />
  </section>
</template>
