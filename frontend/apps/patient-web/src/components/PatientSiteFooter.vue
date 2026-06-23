<template>
  <footer class="site-footer">
    <div class="site-footer-grid">
      <section>
        <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
          <span>智慧云脑</span>
          <i></i><i></i>
        </RouterLink>
        <p>以医院官网为入口，将患者服务、医生科室、健康资料和 AI 辅助能力整合在同一套专业视觉体系中。</p>
      </section>
      <nav aria-label="页脚患者服务">
        <strong>患者服务</strong>
        <RouterLink :to="{ name: 'patient-doctors' }">在线挂号</RouterLink>
        <RouterLink :to="{ name: 'patient-appointments' }">我的预约</RouterLink>
        <RouterLink :to="{ name: 'patient-records' }">我的病历</RouterLink>
        <RouterLink :to="{ name: 'patient-prescriptions' }">我的处方</RouterLink>
      </nav>
      <nav aria-label="页脚医疗资源">
        <strong>医疗资源</strong>
        <RouterLink :to="{ name: 'public-departments' }">科室导航</RouterLink>
        <RouterLink :to="{ name: 'public-search', query: { q: '医生' } }">查找医生</RouterLink>
        <RouterLink :to="{ name: 'public-conditions' }">健康资料库</RouterLink>
        <RouterLink :to="{ name: 'service-emergency' }">急诊指南</RouterLink>
      </nav>
      <nav aria-label="页脚关于医院">
        <strong>关于智慧云脑</strong>
        <RouterLink :to="{ name: 'about-hospital' }">医院介绍</RouterLink>
        <RouterLink :to="{ name: 'public-research' }">科研与教育</RouterLink>
        <RouterLink :to="{ name: 'about-careers' }">招聘信息</RouterLink>
        <RouterLink :to="{ name: 'about-contact' }">联系我们</RouterLink>
      </nav>
    </div>
    <p class="site-footer-meta">简体中文 · 使用条款 · 隐私政策 · 数字无障碍声明 · © 2026 智慧云脑</p>
  </footer>
</template>
