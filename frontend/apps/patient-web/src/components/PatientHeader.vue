<script setup lang="ts">
defineProps<{ userName?: string; publicMode?: boolean }>();
defineEmits<{ logout: [] }>();
</script>

<template>
  <header class="mayo-header">
    <div class="mayo-utility">
      <RouterLink to="/">患者服务</RouterLink>
      <RouterLink to="/triage">症状分诊</RouterLink>
      <RouterLink to="/records">病历处方</RouterLink>
      <span v-if="userName">{{ userName }} · 已登录</span>
    </div>
    <div class="mayo-mainbar">
      <RouterLink class="mayo-brand" to="/" aria-label="Smart Cloud Brain 患者端">
        <span class="mayo-symbol">SCB</span>
        <span>Smart Cloud Brain</span>
      </RouterLink>
      <nav v-if="!publicMode" class="mayo-nav" aria-label="患者服务导航">
        <RouterLink to="/triage">Request appointment</RouterLink>
        <RouterLink to="/doctors">Find a doctor</RouterLink>
        <RouterLink to="/appointments">Appointments</RouterLink>
        <RouterLink to="/records">Patient portal</RouterLink>
        <RouterLink to="/profile">Profile</RouterLink>
      </nav>
      <div class="mayo-actions">
        <RouterLink v-if="publicMode" class="button" to="/register">注册</RouterLink>
        <RouterLink v-if="publicMode" class="button primary" to="/login">登录</RouterLink>
        <RouterLink v-else class="button primary" to="/triage">开始预约</RouterLink>
        <button v-if="!publicMode" type="button" @click="$emit('logout')">退出</button>
      </div>
    </div>
  </header>
</template>
