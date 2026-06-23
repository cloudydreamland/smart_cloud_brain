/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
    "../../packages/shared-ui/src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  safelist: [
    // Modal 组件动态生成的类（在 computed 里，Tailwind 扫描不到）
    "z-[101]",
    "bg-black/60",
    "max-w-[400px]",
    "max-w-[520px]",
    "max-w-[680px]",
    "max-h-[calc(100vh-2rem)]",
    // Transition 动画类
    "opacity-0",
    "opacity-100",
    "scale-95",
    "scale-100",
    "translate-y-0",
    "translate-y-2",
    "transition-opacity",
    "transition-all",
    "duration-150",
    "duration-200",
    "ease-in",
    "ease-out",
  ],
  corePlugins: {
    preflight: false, // 禁用 Tailwind 样式重置，避免覆盖项目自定义 CSS
  },
  theme: {
    extend: {},
  },
  plugins: [],
};
