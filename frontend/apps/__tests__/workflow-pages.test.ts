// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest";
import { shallowMount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import PatientLoginPage from "../patient-web/src/pages/LoginPage.vue";
import TriagePage from "../patient-web/src/pages/TriagePage.vue";
import ConsultationPage from "../doctor-web/src/pages/ConsultationPage.vue";
import SchedulePage from "../admin-web/src/pages/SchedulePage.vue";
import AccountsPage from "../admin-web/src/pages/AccountsPage.vue";
import DevicesPage from "../admin-web/src/pages/DevicesPage.vue";
import PatientsPage from "../admin-web/src/pages/PatientsPage.vue";
import StatisticsPage from "../admin-web/src/pages/StatisticsPage.vue";
import PermissionsPage from "../admin-web/src/pages/PermissionsPage.vue";
import { useDoctorWorkflowStore } from "../../packages/shared-api/src/index";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
}));

describe("closed-loop page smoke tests", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it("renders patient login", () => {
    const wrapper = shallowMount(PatientLoginPage, { global: { stubs: { RouterLink: true } } });
    expect(wrapper.text()).toContain("确认您的患者身份");
  });

  it("renders patient triage", () => {
    const wrapper = shallowMount(TriagePage);
    expect(wrapper.text()).toContain("智能分诊");
    expect(wrapper.text()).toContain("提交分诊");
  });

  it("keeps patient triage submit available before validation", () => {
    const wrapper = shallowMount(TriagePage);
    expect((wrapper.get("button.primary").element as HTMLButtonElement).disabled).toBe(false);
  });

  it("renders doctor consultation", () => {
    const workflow = useDoctorWorkflowStore();
    workflow.registrations = [{ registrationId: 1, patientId: 1, patientName: "患者" }];
    const wrapper = shallowMount(ConsultationPage, { props: { registrationId: "1" } });
    expect(wrapper.text()).toContain("病历工作区");
    expect(wrapper.text()).toContain("患者分诊");
  });

  it("renders administrator AI schedule", () => {
    const wrapper = shallowMount(SchedulePage);
    expect(wrapper.text()).toContain("Doctor Schedule Management");
    expect(wrapper.text()).toContain("Generate");
  });

  it("renders administrator account permissions", () => {
    const wrapper = shallowMount(AccountsPage);
    expect(wrapper.text()).toContain("账户与权限管理");
    expect(wrapper.text()).toContain("新增账户");
  });

  it("renders administrator device management", () => {
    const wrapper = shallowMount(DevicesPage);
    expect(wrapper.text()).toContain("Medical Device Management");
    expect(wrapper.text()).toContain("New Device");
  });

  it("renders administrator patient management", () => {
    const wrapper = shallowMount(PatientsPage);
    expect(wrapper.text()).toContain("Patient Management");
    expect(wrapper.text()).toContain("Search");
  });

  it("renders administrator statistics", () => {
    const wrapper = shallowMount(StatisticsPage);
    expect(wrapper.text()).toContain("Diagnosis Data Statistics");
    expect(wrapper.text()).toContain("CSV");
  });

  it("renders administrator permissions", () => {
    const wrapper = shallowMount(PermissionsPage);
    expect(wrapper.text()).toContain("Role Permissions");
    expect(wrapper.text()).toContain("Save");
  });
});
