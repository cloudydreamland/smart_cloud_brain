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
      <text class="section-title">筛选号源</text>
      <picker mode="selector" :range="departmentLabels" :value="departmentIndex" @change="onDepartmentChange">
        <view class="picker">{{ departmentLabels[departmentIndex] || "全部科室" }}</view>
      </picker>
      <input v-model.trim="query" placeholder="搜索医生、科室或日期" />
      <view class="record" v-if="guidanceText">
        <text class="tag">分诊推荐</text>
        <text class="muted">{{ guidanceText }}</text>
      </view>
      <button class="ghost" :disabled="busy" @click="refresh">刷新号源</button>
    </view>

    <view class="panel">
      <text class="section-title">可约号源</text>
      <view class="segmented" v-if="dateGroups.length">
        <button v-for="group in dateGroups" :key="group.date" class="segment" :class="{ active: selectedDate === group.date }" @click="selectedDate = group.date">{{ group.label }} {{ group.slots.length }}</button>
      </view>
      <view v-for="doctor in doctorGroups" :key="doctor.key" class="record">
        <text class="strong">{{ doctor.doctorName }} · {{ doctor.departmentName }}</text>
        <text class="muted">共 {{ doctor.slots.length }} 个时段</text>
        <view v-for="period in doctor.periods" :key="doctor.key + period.key" class="stack">
          <text class="tag">{{ period.label }}</text>
          <view v-for="slot in period.slots" :key="String(slot.slotId || slot.scheduleId || slot.startTime)" class="record">
            <text class="strong">{{ slotTimeText(slot) }}</text>
            <text class="muted">{{ dateText(slot.startTime || slot.appointmentTime) }} · 余号 {{ slot.remainingCapacity == null ? "-" : slot.remainingCapacity }}/{{ slot.capacity == null ? "-" : slot.capacity }}</text>
            <button :disabled="busy || !isSlotBookable(slot)" @click="confirm(slot)">{{ isSlotBookable(slot) ? "确认预约" : "暂不可约" }}</button>
          </view>
        </view>
      </view>
      <text class="empty" v-if="!displaySlots.length">暂无可约号源，请在管理端发布排班后刷新。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { dateText, latestById, list, num } from "../../common/formatters.js";

export default {
  data() {
    return { departments: [], slots: [], triages: [], departmentIndex: 0, query: "", selectedDate: "", initialDepartment: "", initialDoctor: "", busy: false, error: "", message: "" };
  },
  computed: {
    departmentLabels() { return ["全部科室", ...this.departments.map((item) => item.name || item.departmentName || "未命名科室")]; },
    selectedDepartment() { return this.departmentIndex <= 0 ? null : this.departments[this.departmentIndex - 1]; },
    latestTriage() { return latestById(this.triages, "triageRecordId"); },
    guidanceText() {
      if (!this.latestTriage) return "";
      const doctor = this.latestTriage.assignedDoctorName || this.latestTriage.doctorName;
      const department = this.latestTriage.recommendedDepartment;
      return doctor ? `已分配医生：${doctor}` : department ? `推荐科室：${department}` : "";
    },
    sourceSlots() {
      let rows = this.slots;
      const assignedDoctorId = num(this.latestTriage && this.latestTriage.assignedDoctorId);
      const recommendedDepartment = String(this.initialDepartment || this.latestTriage && this.latestTriage.recommendedDepartment || "");
      if (this.initialDoctor) rows = rows.filter((slot) => String(slot.doctorId) === String(this.initialDoctor));
      else if (assignedDoctorId) rows = rows.filter((slot) => num(slot.doctorId) === assignedDoctorId);
      else if (recommendedDepartment) rows = rows.filter((slot) => String(slot.departmentName || "").includes(recommendedDepartment));
      return rows.length ? rows : this.slots;
    },
    displaySlots() {
      let rows = this.sourceSlots;
      if (this.selectedDepartment) {
        const id = String(this.selectedDepartment.departmentId || this.selectedDepartment.id || "");
        const name = String(this.selectedDepartment.name || this.selectedDepartment.departmentName || "");
        rows = rows.filter((slot) => String(slot.departmentId || "") === id || String(slot.departmentName || "").includes(name));
      }
      const q = this.query.trim().toLowerCase();
      if (q) rows = rows.filter((slot) => `${slot.departmentName || ""} ${slot.doctorName || ""} ${slot.startTime || ""}`.toLowerCase().includes(q));
      return [...rows].sort((left, right) => this.slotTimestamp(left) - this.slotTimestamp(right));
    },
    dateGroups() {
      const groups = new Map();
      this.displaySlots.forEach((slot) => {
        const date = this.slotDateKey(slot.startTime || slot.appointmentTime || "");
        if (!groups.has(date)) groups.set(date, { date, label: this.slotDateLabel(date), slots: [] });
        groups.get(date).slots.push(slot);
      });
      return [...groups.values()];
    },
    activeSlots() {
      const group = this.dateGroups.find((item) => item.date === this.selectedDate) || this.dateGroups[0];
      return group ? group.slots : [];
    },
    doctorGroups() {
      const groups = new Map();
      this.activeSlots.forEach((slot) => {
        const key = String(slot.doctorId || slot.doctorName || "unknown");
        if (!groups.has(key)) groups.set(key, { key, doctorName: slot.doctorName || "未命名医生", departmentName: slot.departmentName || "未定科室", slots: [] });
        groups.get(key).slots.push(slot);
      });
      return [...groups.values()].map((doctor) => ({ ...doctor, periods: this.periodGroups(doctor.slots) }));
    }
  },
  watch: {
    dateGroups(groups) {
      if (!groups.length) this.selectedDate = "";
      else if (!groups.some((item) => item.date === this.selectedDate)) this.selectedDate = groups[0].date;
    }
  },
  onLoad(options) {
    this.initialDepartment = options && options.department ? decodeURIComponent(options.department) : "";
    this.initialDoctor = options && options.doctor ? decodeURIComponent(options.doctor) : "";
    this.query = this.initialDepartment;
  },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    dateText,
    slotTimestamp(slot) {
      const value = Date.parse(slot.startTime || slot.appointmentTime || "");
      return Number.isFinite(value) ? value : Number.MAX_SAFE_INTEGER;
    },
    slotDateKey(time) { return time ? String(time).slice(0, 10) : "未定日期"; },
    slotDateLabel(date) { return date === "未定日期" ? date : date.slice(5); },
    slotTimeText(slot) {
      const value = String(slot.startTime || slot.appointmentTime || "");
      const time = value.includes("T") ? value.split("T")[1] : value.split(" ")[1] || value;
      return time ? time.slice(0, 5) : "待定";
    },
    slotPeriod(time) {
      const raw = String(time || "").includes("T") ? String(time).split("T")[1] : String(time || "").split(" ")[1] || "";
      const hour = Number(raw.slice(0, 2));
      if (!Number.isFinite(hour)) return { key: "unknown", label: "待定时段" };
      if (hour < 12) return { key: "morning", label: "上午" };
      if (hour < 18) return { key: "afternoon", label: "下午" };
      return { key: "evening", label: "晚上" };
    },
    periodGroups(slots) {
      const groups = new Map();
      slots.forEach((slot) => {
        const period = this.slotPeriod(slot.startTime || slot.appointmentTime);
        if (!groups.has(period.key)) groups.set(period.key, { ...period, slots: [] });
        groups.get(period.key).slots.push(slot);
      });
      const order = { morning: 1, afternoon: 2, evening: 3, unknown: 4 };
      return [...groups.values()].sort((a, b) => order[a.key] - order[b.key]);
    },
    onDepartmentChange(event) { this.departmentIndex = Number(event.detail.value); },
    isSlotBookable(slot) {
      const left = slot.remainingCapacity == null ? 1 : num(slot.remainingCapacity);
      return Boolean(num(slot.doctorId) && num(slot.departmentId) && String(slot.startTime || slot.appointmentTime || "") && num(slot.slotId || slot.scheduleId) && left > 0);
    },
    async refresh() {
      this.busy = true;
      this.error = "";
      try {
        const [departments, slots, triages] = await Promise.all([api.departments().catch(() => []), api.slots(), api.triageList().catch(() => [])]);
        this.departments = list(departments);
        this.slots = list(slots);
        this.triages = list(triages);
      } catch (error) {
        this.error = error.message || "号源加载失败";
      } finally {
        this.busy = false;
      }
    },
    confirm(slot) {
      if (!this.isSlotBookable(slot)) {
        this.error = "当前号源信息不完整或余号不足，请刷新后重试";
        return;
      }
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
              triageRecordId: num(this.latestTriage && this.latestTriage.triageRecordId) || null,
              slotId: num(slot.slotId || slot.scheduleId) || null
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
