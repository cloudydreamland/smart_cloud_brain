// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount, shallowMount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { defineComponent, nextTick, ref } from "vue";
import { api, type Device } from "../../packages/shared-api/src/index";
import RowActionMenu from "../admin-web/src/components/RowActionMenu.vue";
import DevicesPage from "../admin-web/src/pages/DevicesPage.vue";

const deviceRows: Device[] = [
  { id: 1, deviceCode: "DEV-AVAILABLE", name: "可用设备", status: "AVAILABLE" },
  { id: 2, deviceCode: "DEV-IN-USE", name: "使用中设备", status: "IN_USE" },
  { id: 3, deviceCode: "DEV-MAINTENANCE", name: "维修设备", status: "MAINTENANCE" },
  { id: 4, deviceCode: "DEV-RETIRED", name: "停用设备", status: "RETIRED" },
];

const RowActionMenuStub = defineComponent({
  name: "RowActionMenu",
  props: {
    items: { type: Array, default: () => [] },
    ariaLabel: { type: String, default: "" },
    disabled: Boolean,
  },
  emits: ["select"],
  template: `
    <div class="row-action-menu-stub" :aria-label="ariaLabel">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        :data-action="item.key"
        :class="{ danger: item.danger }"
        :disabled="disabled || item.disabled"
        @click="$emit('select', item.key)"
      >
        {{ item.label }}
      </button>
    </div>
  `,
});

describe("device row actions", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.restoreAllMocks();
    vi.spyOn(api, "devices").mockResolvedValue(deviceRows);
    vi.spyOn(api, "deviceUsages").mockResolvedValue([]);
    vi.spyOn(api, "updateDeviceStatus").mockResolvedValue({});
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("keeps frequent actions visible and varies overflow actions by status", async () => {
    const wrapper = shallowMount(DevicesPage, {
      global: {
        provide: { toast: ref(null) },
        stubs: { RowActionMenu: RowActionMenuStub },
      },
    });
    await flushPromises();

    const rows = wrapper.findAll("tbody tr");
    expect(rows).toHaveLength(4);

    expect(rows[0].findAll(".device-row-action").map((button) => button.text())).toEqual(["使用记录", "编辑"]);
    expect(rows[0].find(".row-action-menu-stub").text()).toContain("设为维修中");
    expect(rows[0].find(".row-action-menu-stub").text()).toContain("停用设备");

    expect(rows[1].findAll(".device-row-action").map((button) => button.text())).toEqual(["使用记录", "编辑"]);
    expect(rows[1].find(".row-action-menu-stub").text()).toContain("设为维修中");

    expect(rows[2].findAll(".device-row-action").map((button) => button.text())).toEqual(["使用记录", "编辑"]);
    expect(rows[2].find(".row-action-menu-stub").text()).toContain("恢复可用");
    expect(rows[2].find(".row-action-menu-stub").text()).toContain("停用设备");

    expect(rows[3].findAll(".device-row-action").map((button) => button.text())).toEqual(["编辑"]);
    expect(rows[3].find(".row-action-menu-stub").exists()).toBe(false);
  });

  it("routes visible and overflow actions through the existing handlers", async () => {
    const wrapper = shallowMount(DevicesPage, {
      global: {
        provide: { toast: ref(null) },
        stubs: { RowActionMenu: RowActionMenuStub },
      },
    });
    await flushPromises();

    const rows = wrapper.findAll("tbody tr");
    await rows[0].findAll(".device-row-action")[0].trigger("click");
    await flushPromises();
    expect(api.deviceUsages).toHaveBeenCalledWith(1);

    await rows[0].get('[data-action="maintain"]').trigger("click");
    await flushPromises();
    expect(api.updateDeviceStatus).toHaveBeenCalledWith({ deviceId: 1, status: "MAINTENANCE" });

    await rows[2].get('[data-action="restore"]').trigger("click");
    await flushPromises();
    expect(api.updateDeviceStatus).toHaveBeenCalledWith({ deviceId: 3, status: "AVAILABLE" });
  });
});

describe("RowActionMenu", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("exposes menu semantics and supports keyboard navigation", async () => {
    const wrapper = mount(RowActionMenu, {
      props: {
        ariaLabel: "64排螺旋CT的更多操作",
        items: [
          { key: "maintain", label: "设为维修中" },
          { key: "retire", label: "停用设备", danger: true, separatorBefore: true },
        ],
      },
      attachTo: document.body,
    });

    const trigger = wrapper.get('button[aria-haspopup="menu"]');
    expect(trigger.attributes("aria-expanded")).toBe("false");
    expect(trigger.attributes("aria-label")).toBe("64排螺旋CT的更多操作");

    await trigger.trigger("keydown", { key: "ArrowDown" });
    await nextTick();

    const menu = document.body.querySelector<HTMLElement>('[role="menu"]');
    const menuItems = Array.from(document.body.querySelectorAll<HTMLButtonElement>('[role="menuitem"]'));
    expect(menu?.getAttribute("aria-label")).toBe("64排螺旋CT的更多操作");
    expect(menu?.style.width).toBe("156px");
    expect(menuItems).toHaveLength(2);
    expect(document.activeElement).toBe(menuItems[0]);

    menuItems[0].dispatchEvent(new KeyboardEvent("keydown", { key: "ArrowDown", bubbles: true }));
    await nextTick();
    expect(document.activeElement).toBe(menuItems[1]);

    menuItems[1].dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", bubbles: true }));
    await nextTick();
    expect(document.body.querySelector('[role="menu"]')).toBeNull();
    expect(document.activeElement).toBe(trigger.element);

    wrapper.unmount();
  });

  it("closes before emitting a selected action", async () => {
    const wrapper = mount(RowActionMenu, {
      props: {
        ariaLabel: "设备的更多操作",
        items: [{ key: "retire", label: "停用设备", danger: true }],
      },
      attachTo: document.body,
    });

    await wrapper.get('button[aria-haspopup="menu"]').trigger("click");
    await nextTick();
    const item = document.body.querySelector<HTMLButtonElement>('[role="menuitem"]');
    item?.click();
    await nextTick();

    expect(wrapper.emitted("select")).toEqual([["retire"]]);
    expect(document.body.querySelector('[role="menu"]')).toBeNull();

    wrapper.unmount();
  });
});
