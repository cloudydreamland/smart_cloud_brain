<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">医生号源</text>
      <text class="title">选择科室和可约号源</text>
      <text class="subtitle">号源来自后端真实排班，确认后会生成预约记录。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="message">{{ message }}</view>

    <view class="panel">
      <text class="section-title">科室筛选</text>
      <picker mode="selector" :range="departmentLabels" :value="departmentIndex" @change="onDepartmentChange">
        <view class="picker">{{ departmentLabels[departmentIndex] || "全部科室" }}</view>
      </picker>
      <button class="ghost" :disabled="busy" @click="refresh">刷新号源</button>
    </view>

    <view class="panel">
      <text class="section-title">可约号源</text>
      <view v-for="slot in filteredSlots" :key="String(slot.slotId || slot.scheduleId || slot.startTime)" class="record">
        <text class="strong">{{ slot.departmentName || "科室" }} · {{ slot.doctorName || "医生" }}</text>
        <text class="muted">{{ dateText(slot.startTime || slot.appointmentTime) }}</text>
        <text class="tag">剩余 {{ slot.remainingCapacity == null ? "-" : slot.remainingCapacity }}</text>
        <button :disabled="busy" @click="confirm(slot)">确认预约</button>
      </view>
      <text class="empty" v-if="!filteredSlots.length">暂无可约号源，请在管理端发布排班后刷新。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { dateText, list, num } from "../../common/formatters.js";

export default {
  data() {
    return { departments: [], slots: [], departmentIndex: 0, busy: false, error: "", message: "" };
  },
  computed: {
    departmentLabels() { return ["全部科室", ...this.departments.map((item) => item.name || item.departmentName || "未命名科室")]; },
    selectedDepartment() { return this.departmentIndex <= 0 ? null : this.departments[this.departmentIndex - 1]; },
    filteredSlots() {
      if (!this.selectedDepartment) return this.slots;
      const id = String(this.selectedDepartment.departmentId || this.selectedDepartment.id || "");
      const name = String(this.selectedDepartment.name || this.selectedDepartment.departmentName || "");
      return this.slots.filter((slot) => String(slot.departmentId || "") === id || String(slot.departmentName || "").includes(name));
    }
  },
  onShow() {
    if (requireLogin()) this.refresh();
  },
  methods: {
    dateText,
    onDepartmentChange(event) { this.departmentIndex = Number(event.detail.value); },
    async refresh() {
      this.busy = true;
      this.error = "";
      try {
        const [departments, slots] = await Promise.all([api.departments().catch(() => []), api.slots()]);
        this.departments = list(departments);
        this.slots = list(slots);
      } catch (error) {
        this.error = error.message || "号源加载失败";
      } finally {
        this.busy = false;
      }
    },
    confirm(slot) {
      uni.showModal({
        title: "确认预约",
        content: `${slot.departmentName || "科室"} ${slot.doctorName || "医生"} ${dateText(slot.startTime || slot.appointmentTime)}`,
        success: async (res) => {
          if (!res.confirm) return;
          this.busy = true;
          this.error = "";
          this.message = "";
          try {
            await api.createRegistration({
              doctorId: num(slot.doctorId),
              departmentId: num(slot.departmentId),
              appointmentTime: String(slot.startTime || slot.appointmentTime || ""),
              triageRecordId: null,
              slotId: num(slot.slotId || slot.scheduleId)
            });
            this.message = "预约已创建";
            await this.refresh();
          } catch (error) {
            this.error = error.message || "预约失败";
          } finally {
            this.busy = false;
          }
        }
      });
    }
  }
};
</script>
