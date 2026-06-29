// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, shallowMount } from "@vue/test-utils";
import { nextTick, ref } from "vue";
import { api, type Schedule } from "../../packages/shared-api/src/index";
import DatePicker from "../../packages/shared-ui/src/components/DatePicker.vue";
import ScbSelect from "../../packages/shared-ui/src/components/ScbSelect.vue";
import SchedulePage from "../doctor-web/src/pages/SchedulePage.vue";

const sampleRows: Schedule[] = [
  {
    id: 1,
    doctorId: 1,
    departmentId: 1,
    departmentName: "心内科",
    workDate: "2026-06-22",
    timeRange: "08:00-12:00",
    capacity: 30,
    booked: 5,
    remainingCapacity: 25,
    status: "PUBLISHED",
  },
  {
    id: 2,
    doctorId: 1,
    departmentId: 1,
    departmentName: "心内科",
    workDate: "2026-06-29",
    timeRange: "14:00-17:00",
    capacity: 20,
    booked: 5,
    remainingCapacity: 15,
    status: "PUBLISHED",
  },
];

function mountPage() {
  return shallowMount(SchedulePage, {
    global: {
      provide: {
        toast: ref({ success: vi.fn(), error: vi.fn() }),
      },
    },
  });
}

describe("doctor schedule page", () => {
  beforeEach(() => {
    vi.useRealTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders shared filters and calculates the current result metrics", async () => {
    vi.spyOn(api, "doctorSchedules").mockResolvedValue(sampleRows);
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.findAllComponents(DatePicker)).toHaveLength(2);
    expect(wrapper.findComponent(ScbSelect).exists()).toBe(true);
    expect(wrapper.find('input[type="date"]').exists()).toBe(false);
    expect(wrapper.text()).toContain("排班数");
    expect(wrapper.text()).toContain("已预约");
    expect(wrapper.text()).toContain("剩余号源");
    expect(wrapper.text()).toContain("预约利用率");
    expect(wrapper.text()).toContain("20%");
    expect(wrapper.text()).toContain("10 / 50");
    wrapper.unmount();
  });

  it("normalizes an inverted date range and reloads filters immediately", async () => {
    const scheduleSpy = vi.spyOn(api, "doctorSchedules").mockResolvedValue(sampleRows);
    const wrapper = mountPage();
    await flushPromises();

    const startPicker = wrapper.findAllComponents(DatePicker)
      .find((picker) => picker.props("ariaLabel") === "排班开始日期");
    if (!startPicker) throw new Error("开始日期组件未渲染");
    startPicker.vm.$emit("update:modelValue", "2026-08-01");
    await nextTick();
    await flushPromises();

    expect(scheduleSpy).toHaveBeenLastCalledWith(expect.objectContaining({
      startDate: "2026-08-01",
      endDate: "2026-08-01",
    }));

    wrapper.findComponent(ScbSelect).vm.$emit("update:modelValue", "CANCELLED");
    await nextTick();
    await flushPromises();
    expect(scheduleSpy).toHaveBeenLastCalledWith(expect.objectContaining({ status: "CANCELLED" }));
    wrapper.unmount();
  });

  it("ignores a stale schedule response after a newer filter request", async () => {
    let resolveFirst!: (value: Schedule[]) => void;
    let resolveSecond!: (value: Schedule[]) => void;
    const first = new Promise<Schedule[]>((resolve) => { resolveFirst = resolve; });
    const second = new Promise<Schedule[]>((resolve) => { resolveSecond = resolve; });
    vi.spyOn(api, "doctorSchedules")
      .mockReturnValueOnce(first)
      .mockReturnValueOnce(second);

    const wrapper = mountPage();
    wrapper.findComponent(ScbSelect).vm.$emit("update:modelValue", "PUBLISHED");
    await nextTick();

    resolveSecond([{ ...sampleRows[0], id: 20, workDate: "2026-07-20" }]);
    await flushPromises();
    resolveFirst([{ ...sampleRows[0], id: 10, workDate: "2026-07-10" }]);
    await flushPromises();

    expect(wrapper.text()).toContain("2026-07-20");
    expect(wrapper.text()).not.toContain("2026-07-10");
    wrapper.unmount();
  });
});
