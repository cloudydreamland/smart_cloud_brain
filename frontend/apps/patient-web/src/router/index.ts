import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import PatientPublicLayout from "../layouts/PatientPublicLayout.vue";
import PatientPortalLayout from "../layouts/PatientPortalLayout.vue";
import LoginPage from "../pages/LoginPage.vue";
import RegisterPage from "../pages/RegisterPage.vue";
import PatientDashboard from "../pages/PatientDashboard.vue";
import TriagePage from "../pages/TriagePage.vue";
import DoctorSlotsPage from "../pages/DoctorSlotsPage.vue";
import AppointmentsPage from "../pages/AppointmentsPage.vue";
import MedicalRecordsPage from "../pages/MedicalRecordsPage.vue";
import PrescriptionsPage from "../pages/PrescriptionsPage.vue";
import ProfilePage from "../pages/ProfilePage.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: PatientPortalLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "", name: "patient-dashboard", component: PatientDashboard },
        { path: "triage", name: "patient-triage", component: TriagePage },
        { path: "doctors", name: "patient-doctors", component: DoctorSlotsPage },
        { path: "appointments", name: "patient-appointments", component: AppointmentsPage },
        { path: "records", name: "patient-records", component: MedicalRecordsPage },
        { path: "prescriptions", name: "patient-prescriptions", component: PrescriptionsPage },
        { path: "profile", name: "patient-profile", component: ProfilePage },
      ],
    },
    {
      path: "/",
      component: PatientPublicLayout,
      children: [
        { path: "login", name: "patient-login", component: LoginPage },
        { path: "register", name: "patient-register", component: RegisterPage },
      ],
    },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  auth.load("patient-session", "PATIENT");
  if (to.meta.requiresAuth && (!auth.session || auth.permissionError)) {
    return { name: "patient-login", query: { redirect: to.fullPath } };
  }
  if ((to.name === "patient-login" || to.name === "patient-register") && auth.session && !auth.permissionError) {
    return { name: "patient-dashboard" };
  }
  return true;
});

export default router;
