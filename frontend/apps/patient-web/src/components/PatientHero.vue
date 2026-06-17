<template>
  <section class="mayo-hero" aria-label="患者端首屏">
    <div>
      <p class="eyebrow">PATIENT WEB</p>
      <h1>让就诊从正确分诊开始</h1>
      <p class="mayo-lead">描述症状，获取 AI 推荐科室，查看可预约号源，并在同一流程确认挂号与诊后记录。</p>
      <div class="patient-hero-actions">
        <RouterLink class="button primary" to="/triage">Request appointment</RouterLink>
        <RouterLink class="button" to="/doctors">Find a doctor</RouterLink>
      </div>
    </div>
    <div class="mayo-image-placeholder" aria-label="医疗服务图片占位"></div>
  </section>
</template>
