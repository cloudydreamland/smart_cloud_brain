<script setup lang="ts">
import { computed, inject, onMounted, ref, watch, type Ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  formatApiError,
  formatDateTime,
  statusClass,
  statusText,
  toNumber,
  usePagination,
  usePatientWorkflowStore,
  type Registration,
} from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, SegmentedControl, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";
import CancelAppointmentModal from "../components/CancelAppointmentModal.vue";

const workflow = usePatientWorkflowStore();
const { registrations } = storeToRefs(workflow);
const statusFilter = ref("TODO");
const loading = ref(false);
const loaded = ref(false);
const payLoading = ref(false);
const cancelLoading = ref(false);
const checkInLoading = ref(false);
const refundLoading = ref(false);
const error = ref("");
const injectedToast = inject<Ref<InstanceType<typeof Toast> | null> | null>("toast", null);
const localToast = ref<InstanceType<typeof Toast> | null>(null);
const toast = computed(() => injectedToast?.value ?? localToast.value);
const selected = ref<Registration | null>(null);
const filterOptions = [
  { label: "全部", value: "" },
  { label: "待处理", value: "TODO" },
  { label: "候诊中", value: "QUEUE" },
  { label: "诊后", value: "AFTER" },
  { label: "已取消", value: "CLOSED" },
];

const sortedRegistrations = computed(() => [...registrations.value].sort((left, right) => {
  return timestamp(right, "createdAt") - timestamp(left, "createdAt")
    || timestamp(right, "appointmentTime") - timestamp(left, "appointmentTime")
    || toNumber(right.registrationId) - toNumber(left.registrationId);
}));
const filteredRegistrations = computed(() => sortedRegistrations.value.filter((item) => {
  if (!statusFilter.value) return true;
  return statusGroup(item.status) === statusFilter.value;
}));
const { currentPage, pageSize, total, pageRows } = usePagination(filteredRegistrations, 8);

watch(statusFilter, () => {
  error.value = "";
  currentPage.value = 1;
});

function timestamp(item: Registration, key: keyof Registration) {
  const value = item[key];
  if (!value) return 0;
  const time = new Date(String(value)).getTime();
  return Number.isFinite(time) ? time : 0;
}

function statusGroup(status: unknown) {
  const value = String(status || "").toUpperCase();
  if (["COMPLETED"].includes(value)) return "AFTER";
  if (["CANCELLED", "NO_SHOW", "REFUNDING", "REFUNDED"].includes(value)) return "CLOSED";
  if (["CHECKED_IN", "WAITING", "CALLED", "IN_CONSULTATION"].includes(value)) return "QUEUE";
  return "TODO";
}

function subjectText(item: Registration) {
  const name = item.subjectName || item.visitorName || item.patientName || "本人";
  const relation = item.subjectRelationship || item.visitorRelationship || "";
  return relation ? `${name}（${relation}）` : name;
}

function nextStepText(item: Registration) {
  const status = String(item.status || "").toUpperCase();
  if (item.canPay) return "请完成支付后再签到";
  if (item.canCheckIn) return "已支付，可在就诊前签到";
  if (["CHECKED_IN", "WAITING"].includes(status)) return "已进入候诊流程，请留意叫号";
  if (status === "CALLED") return "已叫号，请尽快前往诊室";
  if (status === "IN_CONSULTATION") return "医生正在接诊";
  if (status === "COMPLETED") return "诊疗已完成，可查看病历或处方";
  if (status === "REFUNDING") return "退费处理中，请等待到账确认";
  if (status === "REFUNDED") return "退费已完成";
  if (status === "CANCELLED") return "预约已取消";
  return "请按页面提示完成下一步";
}

function formatAmount(item: Registration) {
  const amount = Number(item.amount ?? 0);
  return Number.isFinite(amount) ? `￥${amount.toFixed(2)}` : "￥0.00";
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refreshAuthenticated();
    loaded.value = true;
  } catch (err) {
    error.value = formatApiError(err, "挂号记录加载失败");
  } finally {
    loading.value = false;
  }
}

async function runAction(
  item: Registration,
  action: (registrationId: number) => Promise<Registration>,
  busy: Ref<boolean>,
  successMessage: string,
  fallback: string,
) {
  busy.value = true;
  error.value = "";
  try {
    await action(toNumber(item.registrationId));
    await refresh();
    toast.value?.success?.("操作成功", successMessage);
  } catch (err) {
    error.value = formatApiError(err, fallback);
    toast.value?.error?.("操作失败", error.value);
  } finally {
    busy.value = false;
  }
}

async function pay(item: Registration) {
  await runAction(item, api.payRegistration, payLoading, "支付状态已更新。", "挂号支付失败");
}

async function checkIn(item: Registration) {
  await runAction(item, api.checkInRegistration, checkInLoading, "签到成功，请留意候诊信息。", "挂号签到失败");
}

async function refund(item: Registration) {
  await runAction(item, api.refundRegistration, refundLoading, "退费状态已更新。", "退费确认失败");
}

async function cancel() {
  if (!selected.value) return;
  await runAction(selected.value, api.cancelRegistration, cancelLoading, "挂号已取消。", "取消挂号失败");
  selected.value = null;
}

onMounted(refresh);
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">我的挂号</p>
        <h2>我的挂号</h2>
        <p>按当前状态查看下一步任务，支付、签到和取消均会同步更新挂号状态。</p>
      </div>
      <button type="button" :disabled="loading" @click="refresh()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ spin: loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新
      </button>
    </header>
    <div class="panel-body stack">
      <div class="toolbar">
        <SegmentedControl v-model="statusFilter" :options="filterOptions" />
        <span class="row-meta">当前 {{ total }} 条 / 全部 {{ registrations.length }} 条</span>
      </div>
      <LoadingState v-if="!loaded && loading" />
      <div v-else-if="error" class="stack">
        <ErrorState :message="error" />
        <button type="button" :disabled="loading" @click="refresh()">重试</button>
      </div>
      <div v-else-if="filteredRegistrations.length" class="list">
        <article v-for="item in pageRows" :key="String(item.registrationId)" class="list-row">
          <div class="row-main">
            <strong>#{{ String(item.registrationId) }} {{ item.departmentName || "" }} · {{ item.doctorName || "" }}</strong>
            <p>就诊人：{{ subjectText(item) }} · 创建时间：{{ formatDateTime(item.createdAt, "暂无") }} · 就诊时间：{{ formatDateTime(item.appointmentTime, "待定") }}</p>
            <p>订单：{{ item.orderNo || "旧挂号记录" }} · {{ statusText(item.paymentStatus, "已预约") }} · {{ formatAmount(item) }}</p>
            <p>{{ nextStepText(item) }}</p>
          </div>
          <div class="toolbar">
            <StatusTag :status="statusText(item.status)" :tone="statusClass(item.status)" />
            <button v-if="item.canPay" type="button" class="primary" :disabled="payLoading" @click="pay(item)">{{ payLoading ? "支付中" : "去支付" }}</button>
            <button v-if="item.canCheckIn" type="button" class="primary" :disabled="checkInLoading" @click="checkIn(item)">{{ checkInLoading ? "签到中" : "签到" }}</button>
            <RouterLink v-if="String(item.status).toUpperCase() === 'COMPLETED'" :to="{ name: 'patient-records' }">查看病历</RouterLink>
            <RouterLink v-if="String(item.status).toUpperCase() === 'COMPLETED'" :to="{ name: 'patient-prescriptions' }">查看处方</RouterLink>
            <button v-if="item.canRefund" type="button" :disabled="refundLoading" @click="refund(item)">{{ refundLoading ? "处理中" : "退费到账" }}</button>
            <button v-if="item.canCancel" class="danger" type="button" :disabled="cancelLoading" @click="selected = item">取消预约</button>
          </div>
        </article>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="暂无挂号记录" message="当前分类下没有挂号记录。" />
    </div>
    <CancelAppointmentModal :open="Boolean(selected)" :busy="cancelLoading" @close="selected = null" @confirm="cancel" />
    <Toast v-if="!injectedToast" ref="localToast" />
  </section>
</template>
