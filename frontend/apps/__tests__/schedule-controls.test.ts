// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount, shallowMount } from "@vue/test-utils";
import { nextTick } from "vue";
import { createPinia, setActivePinia } from "pinia";
import { api } from "../../packages/shared-api/src/index";
import DatePicker from "../../packages/shared-ui/src/components/DatePicker.vue";
import DepartmentDoctorCascader from "../admin-web/src/components/DepartmentDoctorCascader.vue";
import SchedulePage from "../admin-web/src/pages/SchedulePage.vue";

const departments = [
  { id: 1, code: "GENERAL", name: "全科门诊" },
  { id: 2, code: "CARDIOLOGY", name: "心内科" },
];

const doctors = [
  { id: 10, name: "李医生", phone: "1", departmentId: 1, title: "主治医师" },
  { id: 20, name: "张医生", phone: "2", departmentId: 2, title: "主任医师" },
];

describe("schedule filter controls", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.body.innerHTML = "";
  });

  it("selects all schedules or a department without forcing a doctor", async () => {
    const wrapper = mount(DepartmentDoctorCascader, {
      attachTo: document.body,
      props: { departmentId: 0, doctorId: 0, departments, doctors },
    });

    await wrapper.get(".schedule-cascader-trigger").trigger("click");
    const departmentButton = Array.from(document.body.querySelectorAll<HTMLButtonElement>(".schedule-cascader-department-select"))
      .find((button) => button.textContent?.includes("心内科"));
    departmentButton?.click();
    expect(wrapper.emitted("update:departmentId")?.at(-1)).toEqual([2]);
    expect(wrapper.emitted("update:doctorId")?.at(-1)).toEqual([0]);

    await wrapper.get(".schedule-cascader-trigger").trigger("click");
    document.body.querySelector<HTMLButtonElement>(".schedule-cascader-all")?.click();
    expect(wrapper.emitted("update:departmentId")?.at(-1)).toEqual([0]);
    expect(wrapper.emitted("update:doctorId")?.at(-1)).toEqual([0]);
    wrapper.unmount();
  });

  it("selects a doctor together with the doctor's department", async () => {
    const wrapper = mount(DepartmentDoctorCascader, {
      attachTo: document.body,
      props: { departmentId: 0, doctorId: 0, departments, doctors },
    });

    await wrapper.get(".schedule-cascader-trigger").trigger("click");
    document.body.querySelector<HTMLButtonElement>('[aria-label="展开心内科医生"]')?.click();
    await nextTick();
    const doctorButton = Array.from(document.body.querySelectorAll<HTMLButtonElement>(".schedule-cascader-doctors button"))
      .find((button) => button.textContent?.includes("张医生"));
    doctorButton?.click();

    expect(wrapper.emitted("update:departmentId")?.at(-1)).toEqual([2]);
    expect(wrapper.emitted("update:doctorId")?.at(-1)).toEqual([20]);
    wrapper.unmount();
  });

  it("reloads immediately and keeps the filter date range valid", async () => {
    const scheduleSpy = vi.spyOn(api, "filteredSchedules").mockResolvedValue([]);
    const wrapper = shallowMount(SchedulePage);
    await flushPromises();

    const datePickers = wrapper.findAllComponents(DatePicker);
    const startDate = datePickers.find((picker) => picker.props("ariaLabel") === "筛选开始日期");
    const endDate = datePickers.find((picker) => picker.props("ariaLabel") === "筛选结束日期");
    if (!startDate || !endDate) throw new Error("排班筛选日期组件未渲染");

    startDate.vm.$emit("update:modelValue", "2026-08-01");
    await nextTick();
    await flushPromises();
    expect(endDate.props("modelValue")).toBe("2026-08-01");
    expect(scheduleSpy).toHaveBeenLastCalledWith(expect.objectContaining({
      startDate: "2026-08-01",
      endDate: "2026-08-01",
    }));

    const cascader = wrapper.findComponent(DepartmentDoctorCascader);
    cascader.vm.$emit("update:departmentId", 2);
    cascader.vm.$emit("update:doctorId", 20);
    await nextTick();
    await flushPromises();
    expect(scheduleSpy).toHaveBeenLastCalledWith(expect.objectContaining({
      departmentId: 2,
      doctorId: 20,
    }));
    wrapper.unmount();
  });
});
