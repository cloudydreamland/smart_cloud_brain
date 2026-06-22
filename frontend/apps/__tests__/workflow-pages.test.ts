// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest";
import { shallowMount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import PatientLoginPage from "../patient-web/src/pages/LoginPage.vue";
import TriagePage from "../patient-web/src/pages/TriagePage.vue";
import ConsultationPage from "../doctor-web/src/pages/ConsultationPage.vue";
import SchedulePage from "../admin-web/src/pages/SchedulePage.vue";
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
    expect(wrapper.text()).toContain("患者登录");
  });

  it("renders patient triage", () => {
    const wrapper = shallowMount(TriagePage);
    expect(wrapper.text()).toContain("智能分诊");
    expect(wrapper.text()).toContain("提交分诊");
  });

  it("renders doctor consultation", () => {
    const workflow = useDoctorWorkflowStore();
    workflow.registrations = [{ registrationId: 1, patientId: 1, patientName: "患者" }];
    const wrapper = shallowMount(ConsultationPage, { props: { registrationId: "1" } });
    expect(wrapper.text()).toContain("病历工作区");
    expect(wrapper.text()).toContain("患者/分诊");
  });

  it("renders administrator AI schedule", () => {
    const wrapper = shallowMount(SchedulePage);
    expect(wrapper.text()).toContain("智能排班与号源发布");
    expect(wrapper.text()).toContain("生成建议");
  });
});
