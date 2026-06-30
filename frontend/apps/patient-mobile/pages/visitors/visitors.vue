<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">家庭成员</text>
      <text class="title">维护就诊人资料</text>
      <text class="subtitle">一个患者账户可以维护本人和家属就诊人。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="message">{{ message }}</view>
    <view class="panel form">
      <text class="section-title">{{ form.id ? "编辑就诊人" : "新增就诊人" }}</text>
      <input v-model.trim="form.name" placeholder="姓名" />
      <picker mode="selector" :range="genderLabels" :value="genderIndex" @change="onGenderChange">
        <view class="picker">{{ genderLabels[genderIndex] }}</view>
      </picker>
      <input v-model.number="form.age" type="number" placeholder="年龄" />
      <input v-model.trim="form.phone" placeholder="手机号" />
      <input v-model.trim="form.relationship" placeholder="关系，例如 本人/父母/子女" />
      <input v-model.trim="form.address" placeholder="地址" />
      <input v-model.trim="form.emergencyContact" placeholder="紧急联系人" />
      <input v-model.trim="form.emergencyPhone" placeholder="紧急联系电话" />
      <picker mode="selector" :range="bloodTypeLabels" :value="bloodTypeIndex" @change="onBloodTypeChange">
        <view class="picker">{{ bloodTypeLabels[bloodTypeIndex] }}</view>
      </picker>
      <input v-model.number="form.heightCm" type="number" placeholder="身高（cm）" />
      <input v-model.number="form.weightKg" type="number" placeholder="体重（kg）" />
      <textarea v-model="form.allergyHistory" placeholder="过敏史" />
      <textarea v-model="form.pastHistory" placeholder="既往史" />
      <button :disabled="busy || !form.name" @click="save">保存</button>
      <button class="ghost" @click="reset">清空</button>
    </view>
    <view class="panel">
      <view class="panel-head">
        <text class="section-title">就诊人列表</text>
        <button class="ghost small" :disabled="busy" @click="refresh()">刷新</button>
      </view>
      <view v-for="item in rows" :key="String(item.id || item.visitorId)" class="record">
        <text class="strong">{{ item.name || "未命名" }}</text>
        <text class="muted">{{ genderText(item.gender) }} · {{ item.age || "-" }} 岁 · {{ relationText(item) }}</text>
        <text class="muted">紧急联系人：{{ item.emergencyContact || "未填写" }} {{ item.emergencyPhone || "" }}</text>
        <text class="muted">血型 {{ item.bloodType || "未说明" }} · 身高 {{ item.heightCm || "0" }} cm · 体重 {{ item.weightKg || "0" }} kg</text>
        <view class="actions" v-if="item.editable !== false">
          <button class="ghost" @click="edit(item)">编辑</button>
          <button class="danger" @click="remove(item)">删除</button>
        </view>
      </view>
      <text class="empty" v-if="!rows.length">暂无家庭成员。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { genderText, list, num } from "../../common/formatters.js";

const emptyForm = () => ({ id: null, name: "", gender: "MALE", age: "", phone: "", relationship: "", address: "", emergencyContact: "", emergencyPhone: "", bloodType: "", heightCm: "", weightKg: "", allergyHistory: "", pastHistory: "" });

export default {
  data() {
    return { rows: [], form: emptyForm(), genders: ["MALE", "FEMALE"], genderLabels: ["男", "女"], genderIndex: 0, bloodTypes: ["", "A", "B", "AB", "O"], bloodTypeLabels: ["血型未说明", "A 型", "B 型", "AB 型", "O 型"], bloodTypeIndex: 0, busy: false, error: "", message: "" };
  },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    genderText,
    onGenderChange(event) { this.genderIndex = Number(event.detail.value); this.form.gender = this.genders[this.genderIndex]; },
    onBloodTypeChange(event) { this.bloodTypeIndex = Number(event.detail.value); this.form.bloodType = this.bloodTypes[this.bloodTypeIndex]; },
    relationText(item) { return item.visitorType === "ACCOUNT" ? "账户本人" : item.relationship || "关系未填"; },
    reset() { this.form = emptyForm(); this.genderIndex = 0; this.bloodTypeIndex = 0; },
    edit(item) {
      this.form = {
        id: item.id || item.visitorId || null,
        name: item.name || "",
        gender: item.gender || "MALE",
        age: item.age || "",
        phone: item.phone || "",
        relationship: item.relationship || "",
        address: item.address || "",
        emergencyContact: item.emergencyContact || "",
        emergencyPhone: item.emergencyPhone || "",
        bloodType: item.bloodType || "",
        heightCm: item.heightCm || "",
        weightKg: item.weightKg || "",
        allergyHistory: item.allergyHistory || "",
        pastHistory: item.pastHistory || ""
      };
      this.genderIndex = this.form.gender === "FEMALE" ? 1 : 0;
      this.bloodTypeIndex = Math.max(0, this.bloodTypes.indexOf(this.form.bloodType));
    },
    async refresh() {
      this.busy = true;
      this.error = "";
      try { this.rows = list(await api.visitors()); }
      catch (error) { this.error = error.message || "就诊人加载失败"; }
      finally { this.busy = false; }
    },
    async save() {
      this.busy = true;
      this.error = "";
      this.message = "";
      try {
        await api.saveVisitor({ ...this.form, age: Number(this.form.age) || 0, heightCm: Number(this.form.heightCm) || 0, weightKg: Number(this.form.weightKg) || 0 });
        this.message = "就诊人已保存";
        this.reset();
        await this.refresh();
      } catch (error) {
        this.error = error.message || "保存失败";
      } finally {
        this.busy = false;
      }
    },
    remove(item) {
      if (item.editable === false) return;
      const id = num(item.id || item.visitorId);
      if (!id) return;
      uni.showModal({
        title: "删除就诊人",
        content: "确认删除这位就诊人吗？",
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await api.deleteVisitor(id);
            await this.refresh();
          } catch (error) {
            this.error = error.message || "删除失败";
          }
        }
      });
    }
  }
};
</script>
