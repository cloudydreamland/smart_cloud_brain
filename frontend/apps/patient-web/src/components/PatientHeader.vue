<script setup lang="ts">
defineProps<{ userName?: string; publicMode?: boolean }>();
defineEmits<{ logout: [] }>();
</script>

<template>
  <header class="mayo-header" :class="{ 'has-mobile-nav': !publicMode }">
    <div class="mayo-utility">
      <RouterLink to="/">患者服务</RouterLink>
      <RouterLink to="/triage">症状分诊</RouterLink>
      <RouterLink to="/records">病历处方</RouterLink>
      <span v-if="userName">{{ userName }} · 已登录</span>
    </div>
    <div class="mayo-mainbar">
      <RouterLink class="mayo-brand" to="/" aria-label="智慧云脑患者端">
        <span class="mayo-symbol">云脑</span>
        <span>智慧云脑</span>
      </RouterLink>
      <nav v-if="!publicMode" class="mayo-nav" aria-label="患者服务导航">
        <RouterLink to="/triage">症状分诊</RouterLink>
        <RouterLink to="/doctors">预约医生</RouterLink>
        <RouterLink to="/appointments">我的挂号</RouterLink>
        <RouterLink to="/records">病历处方</RouterLink>
        <RouterLink to="/profile">个人资料</RouterLink>
      </nav>
      <div class="mayo-actions">
        <RouterLink v-if="publicMode" class="button" to="/register">注册</RouterLink>
        <RouterLink v-if="publicMode" class="button primary" to="/login">登录</RouterLink>
        <RouterLink v-else class="button primary header-cta" to="/triage">开始预约</RouterLink>
        <button v-if="!publicMode" type="button" @click="$emit('logout')">退出</button>
      </div>
    </div>
    <nav v-if="!publicMode" class="mobile-patient-nav" aria-label="患者移动端导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/triage">分诊</RouterLink>
      <RouterLink to="/doctors">预约</RouterLink>
      <RouterLink to="/appointments">挂号</RouterLink>
      <RouterLink to="/records">记录</RouterLink>
      <RouterLink to="/profile">我的</RouterLink>
    </nav>
  </header>
</template>
