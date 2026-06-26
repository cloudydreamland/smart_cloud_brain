<script setup lang="ts">
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, toNumber, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState, SegmentedControl } from "@smart-cloud-brain/shared-ui";

type InvoiceRow = {
  id: string;
  category: "门诊" | "检查" | "处方";
  title: string;
  date: string;
  status: "待开具" | "可申请" | "已归档";
  amountText: string;
  description: string;
};

const workflow = usePatientWorkflowStore();
const { registrations, prescriptions } = storeToRefs(workflow);
const category = ref("");
const selected = ref<InvoiceRow | null>(null);
const options = [
  { label: "全部", value: "" },
  { label: "门诊", value: "门诊" },
  { label: "检查", value: "检查" },
  { label: "处方", value: "处方" },
];

const invoiceRows = computed<InvoiceRow[]>(() => {
  const registrationRows = registrations.value.map((item, index) => ({
    id: `registration-${fieldText(item, "registrationId", String(index + 1))}`,
    category: "门诊" as const,
    title: `${fieldText(item, "departmentName", "门诊")} · ${fieldText(item, "doctorName", "医生待定")}`,
    date: fieldText(item, "appointmentTime", fieldText(item, "createdAt", "时间待同步")),
    status: fieldText(item, "status") === "COMPLETED" ? "可申请" as const : "待开具" as const,
    amountText: "以财务系统为准",
    description: "门诊挂号费用和相关票据状态以医院财务系统最终记录为准。",
  }));
  const prescriptionRows = prescriptions.value.map((item, index) => ({
    id: `prescription-${fieldText(item, "prescriptionId", String(index + 1))}`,
    category: "处方" as const,
    title: `处方 #${fieldText(item, "prescriptionId", String(index + 1))}`,
    date: fieldText(item, "createdAt", "时间待同步"),
    status: toNumber(item.prescriptionId) ? "已归档" as const : "待开具" as const,
    amountText: "药品费用待同步",
    description: fieldText(item, "riskLevel", "处方记录已归档，费用明细以后端财务接口为准。"),
  }));
  return [...registrationRows, ...prescriptionRows];
});
const filteredRows = computed(() => invoiceRows.value.filter((item) => !category.value || item.category === category.value));
const guideCards = [
  { title: "票据范围", text: "当前页面整理门诊、检查和处方相关费用线索，正式票据以后端财务系统为准。" },
  { title: "抬头信息", text: "开票前请核对个人或单位抬头、税号、联系方式和接收邮箱。" },
  { title: "隐私保护", text: "票据与就诊资料均属于敏感信息，登录后仅展示本人或授权就诊人资料。" },
];
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">电子发票</p>
        <h2>费用与票据</h2>
        <p>按就诊记录和处方记录整理票据线索，便于后续接入正式财务接口。</p>
      </div>
    </header>
    <div class="panel-body stack">
      <div class="toolbar">
        <SegmentedControl v-model="category" :options="options" />
        <span class="row-meta">当前 {{ filteredRows.length }} 条 / 全部 {{ invoiceRows.length }} 条</span>
      </div>

      <div v-if="filteredRows.length" class="invoice-list">
        <article v-for="item in filteredRows" :key="item.id" class="invoice-row">
          <div>
            <span>{{ item.category }} · {{ item.date }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </div>
          <div>
            <strong>{{ item.amountText }}</strong>
            <em>{{ item.status }}</em>
            <button type="button" @click="selected = item">查看</button>
          </div>
        </article>
      </div>
      <EmptyState v-else title="暂无票据记录" message="完成挂号、检查或处方后，相关费用线索会显示在这里。" />

      <div class="portal-guide-grid">
        <article v-for="item in guideCards" :key="item.title">
          <h3>{{ item.title }}</h3>
          <p>{{ item.text }}</p>
        </article>
      </div>

      <div v-if="selected" class="portal-detail-drawer" role="dialog" aria-modal="true">
        <button type="button" aria-label="关闭" @click="selected = null">×</button>
        <span>{{ selected.category }} · {{ selected.status }}</span>
        <h2>{{ selected.title }}</h2>
        <p>{{ selected.description }}</p>
        <ul>
          <li>正式金额、开票状态和票据下载以后端财务系统返回为准。</li>
          <li>如需单位抬头，请在申请前核对名称、税号和联系方式。</li>
          <li>涉及退费、补打或抬头修改，请按医院财务窗口规则处理。</li>
        </ul>
      </div>
    </div>
  </section>
</template>
