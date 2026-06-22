import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import DoctorLoginPage from "../pages/DoctorLoginPage.vue";
import DoctorWorkspaceLayout from "../layouts/DoctorWorkspaceLayout.vue";
import DoctorDashboard from "../pages/DoctorDashboard.vue";
import QueuePage from "../pages/QueuePage.vue";
import ConsultationPage from "../pages/ConsultationPage.vue";
import RecordsPage from "../pages/RecordsPage.vue";
import PrescriptionsPage from "../pages/PrescriptionsPage.vue";
import NotificationsPage from "../pages/NotificationsPage.vue";
import DoctorSettingsPage from "../pages/DoctorSettingsPage.vue";
import SchedulePage from "../pages/SchedulePage.vue";

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
