<script setup lang="ts">
import { computed } from "vue";
import { defaultPatientSiteConfig, type PatientFooterConfig } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../site-config/routeTarget";
import { usePatientSiteConfig } from "../site-config/usePatientSiteConfig";

const { config } = usePatientSiteConfig();

const footer = computed<PatientFooterConfig>(() => {
  const current = config.value.footer;
  return current.brandName ? current : defaultPatientSiteConfig.footer;
});

const footerLinks = computed(() => footer.value.links.filter((link) => link.enabled !== false));
const legalLinks = computed(() => footer.value.legalLinks.filter((link) => link.enabled !== false));
</script>

<template>
  <footer class="site-footer">
    <div class="site-footer-grid">
      <section>
        <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
          <span>{{ footer.brandName }}</span>
          <i></i><i></i>
        </RouterLink>
        <p>{{ footer.description }}</p>
        <p v-if="footer.contactPhone || footer.contactAddress" class="site-footer-contact">
          {{ [footer.contactPhone, footer.contactAddress].filter(Boolean).join(" · ") }}
        </p>
      </section>
      <nav aria-label="页脚患者服务">
        <strong>常用入口</strong>
        <RouterLink v-for="link in footerLinks" :key="`${link.routeName}-${link.slug || link.label}`" :to="toPatientRoute(link)">
          {{ link.label }}
        </RouterLink>
      </nav>
      <nav v-if="legalLinks.length" aria-label="页脚法律链接">
        <strong>法律信息</strong>
        <RouterLink v-for="link in legalLinks" :key="`${link.routeName}-${link.slug || link.label}`" :to="toPatientRoute(link)">
          {{ link.label }}
        </RouterLink>
      </nav>
    </div>
    <p class="site-footer-meta">简体中文 · 数字无障碍声明 · {{ footer.copyright }}</p>
  </footer>
</template>
