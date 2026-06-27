import { describe, expect, it } from "vitest";
import { patientSiteSectionTypes } from "@smart-cloud-brain/shared-api";
import { sectionEditorMap } from "./sectionEditorMap";

describe("sectionEditorMap", () => {
  it("provides one editor component for every CMS section type", () => {
    expect(Object.keys(sectionEditorMap).sort()).toEqual([...patientSiteSectionTypes].sort());
    patientSiteSectionTypes.forEach((type) => {
      expect(sectionEditorMap[type]).toBeTruthy();
    });
  });
});
