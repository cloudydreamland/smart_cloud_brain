import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";

import AdminWorkspaceLayout from "../layouts/AdminWorkspaceLayout.vue";
import { hasAdminPermission, loadAdminPermissions, PATIENT_SITE_MANAGE_PERMISSION } from "../adminPermissions";

const AdminLoginPage = () => import("../pages/AdminLoginPage.vue");
const AdminDashboard = () => import("../pages/AdminDashboard.vue");
const DepartmentsPage = () => import("../pages/DepartmentsPage.vue");
const DoctorsPage = () => import("../pages/DoctorsPage.vue");
const DrugsPage = () => import("../pages/DrugsPage.vue");
const SchedulePage = () => import("../pages/SchedulePage.vue");
const TriageDeskPage = () => import("../pages/TriageDeskPage.vue");
const AccountsPage = () => import("../pages/AccountsPage.vue");
const DevicesPage = () => import("../pages/DevicesPage.vue");
const PatientsPage = () => import("../pages/PatientsPage.vue");
const PermissionsPage = () => import("../pages/PermissionsPage.vue");
const PatientSiteConfigPage = () => import("../pages/PatientSiteConfigPage.vue");
const EmailConfigPage = () => import("../pages/EmailConfigPage.vue");
const NotFound = () => import("../pages/NotFound.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "admin-login", component: AdminLoginPage },
    {
      path: "/",
      component: AdminWorkspaceLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "", name: "admin-dashboard", component: AdminDashboard },
        { path: "departments", name: "admin-departments", component: DepartmentsPage },
        { path: "doctors", name: "admin-doctors", component: DoctorsPage },
        { path: "drugs", name: "admin-drugs", component: DrugsPage },
        { path: "schedule", name: "admin-schedule", component: SchedulePage },
        { path: "triage-desk", name: "admin-triage", component: TriageDeskPage },
        { path: "devices", name: "admin-devices", component: DevicesPage },
        { path: "patients", name: "admin-patients", component: PatientsPage },
        { path: "accounts", name: "admin-accounts", component: AccountsPage },
        { path: "permissions", name: "admin-permissions", component: PermissionsPage },
        { path: "patient-site", name: "admin-patient-site", component: PatientSiteConfigPage, meta: { permission: PATIENT_SITE_MANAGE_PERMISSION } },
        { path: "email-config", name: "admin-email-config", component: EmailConfigPage },
      ],
    },
    { path: "/:pathMatch(.*)*", name: "not-found", component: NotFound },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  auth.load("admin-session", "ADMIN");
  if (to.meta.requiresAuth && (!auth.session || auth.permissionError)) return { name: "admin-login", query: { redirect: to.fullPath } };
  if (to.name === "admin-login" && auth.session && !auth.permissionError) return { name: "admin-dashboard" };
  const permission = typeof to.meta.permission === "string" ? to.meta.permission : "";
  if (auth.session && permission) {
    const permissions = await loadAdminPermissions();
    if (!hasAdminPermission(permissions, permission)) return { name: "admin-dashboard", query: { denied: permission } };
  }
  return true;
});

export default router;
