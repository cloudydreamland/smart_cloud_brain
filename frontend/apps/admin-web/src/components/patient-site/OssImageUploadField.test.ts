import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import OssImageUploadField from "./OssImageUploadField.vue";

const uploadImage = vi.fn();

vi.mock("../../composables/useOssImageUpload", () => ({
  useOssImageUpload: () => ({
    uploading: ref(false),
    error: ref(""),
    uploadImage,
  }),
}));

describe("OssImageUploadField", () => {
  it("emits uploaded asset and v-model writeback fields", async () => {
    uploadImage.mockResolvedValueOnce({
      url: "https://cdn.example.com/patient-site/demo.webp",
      objectKey: "patient-site/demo.webp",
    });
    const wrapper = mount(OssImageUploadField, {
      props: { label: "图片", imageUrl: "", imageAlt: "", objectKey: "" },
    });

    const input = wrapper.find("input[type='file']");
    Object.defineProperty(input.element, "files", {
      configurable: true,
      value: [new File(["demo"], "demo.webp", { type: "image/webp" })],
    });
    await input.trigger("change");

    expect(wrapper.emitted("uploaded")?.[0]?.[0]).toEqual({
      url: "https://cdn.example.com/patient-site/demo.webp",
      objectKey: "patient-site/demo.webp",
    });
    expect(wrapper.emitted("update:imageUrl")?.[0]).toEqual(["https://cdn.example.com/patient-site/demo.webp"]);
    expect(wrapper.emitted("update:objectKey")?.[0]).toEqual(["patient-site/demo.webp"]);
  });

  it("clears image url, object key and alt together", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const wrapper = mount(OssImageUploadField, {
      props: {
        label: "图片",
        imageUrl: "https://cdn.example.com/old.webp",
        imageAlt: "旧图",
        objectKey: "patient-site/old.webp",
      },
    });

    await wrapper.find("button.danger-link").trigger("click");

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining("确认清空图片"));
    expect(wrapper.emitted("cleared")).toHaveLength(1);
    expect(wrapper.emitted("update:imageUrl")?.[0]).toEqual([""]);
    expect(wrapper.emitted("update:objectKey")?.[0]).toEqual([""]);
    expect(wrapper.emitted("update:imageAlt")?.[0]).toEqual([""]);
    confirm.mockRestore();
  });

  it("keeps image fields when clearing is cancelled", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    const wrapper = mount(OssImageUploadField, {
      props: {
        label: "品牌 Logo",
        imageUrl: "https://cdn.example.com/logo.webp",
        imageAlt: "Logo",
        objectKey: "patient-site/logo.webp",
      },
    });

    await wrapper.find("button.danger-link").trigger("click");

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining("确认清空品牌 Logo"));
    expect(wrapper.emitted("cleared")).toBeUndefined();
    expect(wrapper.emitted("update:imageUrl")).toBeUndefined();
    expect(wrapper.emitted("update:objectKey")).toBeUndefined();
    expect(wrapper.emitted("update:imageAlt")).toBeUndefined();
    confirm.mockRestore();
  });
});
