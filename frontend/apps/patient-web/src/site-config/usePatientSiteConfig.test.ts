import { describe, expect, it } from "vitest";
import { normalizeConfig } from "./usePatientSiteConfig";

describe("normalizeConfig", () => {
  it("falls back to default config for empty payloads", () => {
    const config = normalizeConfig({});
    expect(config.nav.menus.length).toBeGreaterThan(0);
    expect(config.home.hero.title).toBeTruthy();
    expect(config.staticPages.pages.length).toBeGreaterThan(0);
  });

  it("keeps every default nav route that is backed by the router whitelist", () => {
    const config = normalizeConfig({});
    const about = config.nav.menus.find((menu) => menu.key === "about");
    expect(about?.links?.some((link) => link.routeName === "public-research")).toBe(true);
  });

  it("filters disabled entries and unknown routes", () => {
    const config = normalizeConfig({
      nav: {
        brand: { name: "Test", homeRoute: "bad-route" },
        menus: [
          {
            key: "x",
            label: "X",
            enabled: true,
            links: [
              { label: "Good", routeName: "patient-home", sort: 20 },
              { label: "Bad", routeName: "bad-route", sort: 10 },
              { label: "Off", routeName: "patient-doctors", enabled: false },
            ],
          },
        ],
      },
      staticPages: {
        pages: [
          { routeName: "bad-route", title: "Bad" },
          { routeName: "about-news", title: "News", enabled: false },
          { routeName: "about-contact", title: "Contact", points: [{ title: "A", text: "B" }] },
        ],
      },
    });

    expect(config.nav.brand.homeRoute).toBe("patient-home");
    expect(config.nav.menus).toHaveLength(1);
    expect(config.nav.menus[0].links).toHaveLength(1);
    expect(config.nav.menus[0].links?.[0].label).toBe("Good");
    expect(config.staticPages.pages.map((page) => page.routeName)).toEqual(["about-contact"]);
  });

  it("sorts nav links and filters quick action routes", () => {
    const config = normalizeConfig({
      home: {
        hero: { title: "Custom" },
        modules: [
          {
            type: "quick_actions",
            key: "actions",
            content: {
              items: [
                { label: "Second", routeName: "patient-triage", sort: 20 },
                { label: "Bad", routeName: "bad-route", sort: 5 },
                { label: "First", routeName: "patient-doctors", sort: 10 },
              ],
            },
          },
        ],
      },
    });

    const actions = config.home.modules[0].content?.items as Array<{ label: string }>;
    expect(config.home.hero.title).toBe("Custom");
    expect(actions.map((item) => item.label)).toEqual(["First", "Second"]);
  });
});
