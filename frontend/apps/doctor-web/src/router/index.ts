import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";

/* Layout（首屏必需，不懒加载） */
import DoctorWorkspaceLayout from "../layouts/DoctorWorkspaceLayout.vue";

/* 页面组件（懒加载，访问时才加载） */
const DoctorLoginPage = () => import("../pages/DoctorLoginPage.vue");
const DoctorDashboard = () => import("../pages/DoctorDashboard.vue");
const QueuePage = () => import("../pages/QueuePage.vue");
const ConsultationPage = () => import("../pages/ConsultationPage.vue");
const RecordsPage = () => import("../pages/RecordsPage.vue");
const PrescriptionsPage = () => import("../pages/PrescriptionsPage.vue");
const NotificationsPage = () => import("../pages/NotificationsPage.vue");
const DoctorSettingsPage = () => import("../pages/DoctorSettingsPage.vue");
const SchedulePage = () => import("../pages/SchedulePage.vue");
const NotFound = () => import("../pages/NotFound.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "doctor-login", component: DoctorLoginPage },
    {
      path: "/",
      component: DoctorWorkspaceLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "", name: "doctor-dashboard", component: DoctorDashboard },
        { path: "queue", name: "doctor-queue", component: QueuePage },
        { path: "consult/:registrationId", name: "doctor-consult", component: ConsultationPage, props: true },
        { path: "records", name: "doctor-records", component: RecordsPage },
        { path: "prescriptions", name: "doctor-prescriptions", component: PrescriptionsPage },
        { path: "schedule", name: "doctor-schedule", component: SchedulePage },
        { path: "notifications", name: "doctor-notifications", component: NotificationsPage },
        { path: "settings", name: "doctor-settings", component: DoctorSettingsPage },
      ],
    },
    /* 404 兜底 */
    { path: "/:pathMatch(.*)*", name: "not-found", component: NotFound },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  auth.load("doctor-session", "DOCTOR");
  if (to.meta.requiresAuth && (!auth.session || auth.permissionError)) return { name: "doctor-login", query: { redirect: to.fullPath } };
  if (to.name === "doctor-login" && auth.session && !auth.permissionError) return { name: "doctor-dashboard" };
  return true;
});

export default router;
