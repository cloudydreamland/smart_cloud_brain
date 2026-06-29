// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import AgeRangeFilter from "../admin-web/src/components/AgeRangeFilter.vue";

describe("AgeRangeFilter", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("combines minimum and maximum ages into one applied filter", async () => {
    const wrapper = mount(AgeRangeFilter, {
      attachTo: document.body,
      props: { minAge: undefined, maxAge: undefined },
    });

    expect(wrapper.get(".age-range-trigger").text()).toContain("全部年龄");
    await wrapper.get(".age-range-trigger").trigger("click");

    const minInput = document.body.querySelector<HTMLInputElement>('input[placeholder="不限"][min="0"]');
    const numberInputs = document.body.querySelectorAll<HTMLInputElement>('input[type="number"]');
    if (!minInput || numberInputs.length !== 2) throw new Error("年龄输入框未渲染");

    minInput.value = "18";
    minInput.dispatchEvent(new Event("input"));
    numberInputs[1].value = "60";
    numberInputs[1].dispatchEvent(new Event("input"));
    await nextTick();

    document.body.querySelector<HTMLButtonElement>(".age-range-popover footer button")?.click();
    await nextTick();
    expect(wrapper.emitted("update:minAge")?.at(-1)).toEqual([18]);
    expect(wrapper.emitted("update:maxAge")?.at(-1)).toEqual([60]);
    wrapper.unmount();
  });

  it("keeps the range ordered and can clear both bounds", async () => {
    const wrapper = mount(AgeRangeFilter, {
      attachTo: document.body,
      props: { minAge: 20, maxAge: 40 },
    });

    await wrapper.get(".age-range-trigger").trigger("click");
    const minInput = document.body.querySelectorAll<HTMLInputElement>('input[type="number"]')[0];
    minInput.value = "80";
    minInput.dispatchEvent(new Event("input"));
    await nextTick();
    expect(minInput.value).toBe("40");

    document.body.querySelector<HTMLButtonElement>(".age-range-popover header button")?.click();
    await nextTick();
    expect(wrapper.emitted("update:minAge")?.at(-1)).toEqual([undefined]);
    expect(wrapper.emitted("update:maxAge")?.at(-1)).toEqual([undefined]);
    wrapper.unmount();
  });
});
