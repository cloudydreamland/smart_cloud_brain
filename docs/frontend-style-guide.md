# 前端样式规范 (Style Guide)

> 最高标准：`UI测试/设计稿/doctor-homepage-redesign.html` 和 `admin-homepage-redesign.html`
> 适用范围：医生端 (doctor-web)、管理端 (admin-web)

---

## 1. 设计令牌 (Design Tokens)

### 颜色

```css
:root {
  /* 背景 */
  --bg: #f7f8fb;              /* 页面底色 */
  --surface: #ffffff;          /* 卡片/面板背景 */
  --surface-2: #f3f5f8;       /* 次级背景（hover、表头、输入框） */

  /* 文字 */
  --ink: #171717;              /* 主文字 */
  --muted: #71717a;            /* 次要文字 */
  --weak: #a1a1aa;             /* 最弱文字（折叠态图标色） */

  /* 边框 */
  --line: #e5e7eb;             /* 通用边框线 */

  /* 语义色 */
  --blue: #2563eb;
  --blue-soft: #eff6ff;
  --green: #16a34a;
  --green-soft: #ecfdf5;
  --red: #dc2626;
  --red-soft: #fef2f2;
  --orange: #ea580c;
  --orange-soft: #fff7ed;

  /* 阴影 */
  --shadow: 0 10px 30px rgba(15, 23, 42, 0.08);

  /* 侧边栏 */
  --sidebar: 56px;             /* 折叠宽度 */
  --sidebar-open: 220px;       /* 展开宽度 */
}
```

### 圆角

| 场景 | 值 | 说明 |
|------|-----|------|
| 卡片/面板 | `14px` | `.panel`、`.metric-card` |
| 按钮/输入框 | `8px` | `.topbar-btn`、`.quick-btn` |
| 导航项 | `8px` | `.nav-item` |
| 标签 | `999px` | `.status-tag`、`.badge`、`.status-pill` |
| 侧边栏标记 | `6px` | `.mark` |

### 字体

```css
font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
font-size: 14px;  /* 基准 */
```

| 场景 | 字号 | 字重 |
|------|------|------|
| 页面大标题 | `28px` | 800 |
| 指标数值 | `28px` | 800, letter-spacing: -0.04em |
| 面板标题 | `14px` | 700 |
| 导航文字 | `13px` | 400 (active: 600) |
| 表格正文 | `13px` | 400 |
| 表头/标签 | `12px` | 600-700 |
| Badge/分组标题 | `10px` | 700-800 |

---

## 2. 侧边栏 (Sidebar)

### 规格

| 属性 | 值 |
|------|-----|
| 折叠宽度 | `56px` |
| 展开宽度 | `220px` |
| 过渡动画 | `width 220ms ease` |
| 背景 | `var(--surface)` 白色 |
| 边框 | `1px solid var(--line)` 右侧 |
| 阴影 | `1px 0 8px rgba(15,23,42,0.04)` |
| z-index | `100` |
| 定位 | `position: fixed` |

### 交互

- **折叠态**：只显示图标（18×18px），文字 opacity:0，badge opacity:0
- **展开触发**：hover 或 class `.open`
- **文字淡入**：`opacity 160ms ease` + `transform: translateX(-8px→0) 160ms ease`
- **导航项 hover**：`background: var(--surface-2)` + `transform: translateX(1px)`
- **Active 状态**：`background: var(--blue-soft)` + `color: var(--blue)` + `font-weight: 600`

### 布局

```
┌─────────────────────────┐
│  org-bar (54px)         │  ← 品牌标记 + 标题
│  border-bottom          │
├─────────────────────────┤
│  nav-wrap (flex:1)      │  ← 可滚动
│    nav-group            │
│      group-label        │  ← 分组标题（可选）
│      nav-item × N       │
│    divider              │  ← 分组间横线
│    nav-group            │
│      ...                │
├─────────────────────────┤
│  sidebar-footer         │  ← 用户头像 + 名称
│  border-top             │
└─────────────────────────┘
```

---

## 3. 顶栏 (Topbar)

### 规格

| 属性 | 值 |
|------|-----|
| 高度 | `56px` |
| 背景 | `rgba(247,248,251,0.86)` + `backdrop-filter: blur(18px)` |
| 边框 | `border-bottom: 1px solid var(--line)` |
| 内边距 | `0 28px` |
| 布局 | `display: flex; justify-content: space-between` |

### 两种风格

**医生端**（`.topbar-left` + `.topbar-right`）：
- 左侧：医生姓名（strong 15px）+ 科室信息（span 12px muted）
- 右侧：状态药丸 × N + 操作按钮

**管理端**（`.topbar-title` + `.topbar-right`）：
- 左侧：标题（h1 16px 700）+ 描述（p 12px muted）
- 右侧：操作按钮

### 状态药丸 (`.status-pill`)

```css
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}
```

绿色圆点（`.dot`）：`7px` 圆形，`background: var(--green)`，外圈光晕 `box-shadow: 0 0 0 4px rgba(40,122,77,0.12)`

---

## 4. 指标卡片 (Metric Cards)

### 规格

| 属性 | 值 |
|------|-----|
| 圆角 | `14px` |
| 边框 | `1px solid rgba(229,231,235,0.8)` |
| 内边距 | `16px 18px` |
| 布局 | `display: grid; gap: 8px` |
| Hover | `transform: translateY(-2px)` + `box-shadow: var(--shadow)` |

### 结构

```
┌─────────────────────┐
│  label    change    │  ← 12px muted, flex justify-between
│  28                  │  ← 28px 800 字重, letter-spacing: -0.04em
└─────────────────────┘
```

### 列数

| 端 | 列数 |
|----|------|
| 医生端 | `repeat(5, 1fr)` |
| 管理端 | `repeat(4, 1fr)` |

---

## 5. 面板 (Panel)

### 规格

| 属性 | 值 |
|------|-----|
| 圆角 | `14px` |
| 边框 | `1px solid rgba(229,231,235,0.8)` |
| 背景 | `var(--surface)` |
| overflow | `hidden` |

### 面板头部 (`.panel-header`)

```css
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
}
.panel-header strong { font-size: 14px; }
.panel-header a { font-size: 12px; color: var(--blue); font-weight: 600; }
```

---

## 6. 表格 (Table)

```css
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th {
  text-align: left;
  color: var(--muted);
  font-weight: 600;
  padding: 10px 14px;
  border-bottom: 1px solid var(--line);
  background: var(--surface-2);
  font-size: 12px;
}
td {
  padding: 11px 14px;
  border-bottom: 1px solid #f0f1f3;
}
tr:hover td { background: #fafbfc; }
```

---

## 7. 状态标签 (Status Tag)

```css
.status-tag {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}
.status-tag.done     { background: var(--green-soft);  color: var(--green); }
.status-tag.cancelled { background: var(--red-soft);    color: var(--red); }
.status-tag.pending  { background: var(--orange-soft); color: var(--orange); }
.status-tag.active   { background: var(--blue-soft);   color: var(--blue); }
```

---

## 8. 按钮

### 顶栏按钮 (`.topbar-btn`)

```css
.topbar-btn {
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
  transition: background 160ms ease;
}
.topbar-btn:hover { background: var(--surface-2); }
```

### 快捷操作按钮 (`.quick-btn`)

```css
.quick-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--surface);
  font-size: 13px;
  font-weight: 600;
  transition: all 160ms ease;
}
.quick-btn:hover {
  background: var(--blue-soft);
  color: var(--blue);
  border-color: var(--blue);
}
```

### 危险按钮

```css
.topbar-btn.danger { color: var(--red); border-color: #fecaca; }
.topbar-btn.danger:hover { background: var(--red-soft); }
```

---

## 9. 布局模式

### 主内容区

```css
.main {
  margin-left: 56px;  /* 固定侧边栏宽度 */
  min-height: 100vh;
  width: calc(100vw - 56px);
  transition: margin-left 220ms ease, width 220ms ease;
  display: flex;
  flex-direction: column;
}
```

### 工作区

```css
.workspace { flex: 1; overflow: auto; padding: 24px 28px; }
.dashboard { max-width: 1200px; display: grid; gap: 20px; }
```

### 双栏布局

| 端 | 比例 |
|----|------|
| 医生端 | `1.6fr 1fr` |
| 管理端 | `1fr 1fr` |

```css
.content-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;  /* 或 1fr 1fr */
  gap: 20px;
  align-items: start;
}
```

---

## 10. 流程步骤条 (Flow Strip) — 仅医生端

```css
.flow-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}
.flow-step {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius, 8px);
  background: rgba(255, 255, 255, 0.8);
}
.flow-step span { color: var(--subtle); font-size: 11px; font-weight: 900; }
.flow-step strong { font-size: 13px; margin-top: 4px; }
.flow-step.done { border-color: #b8dcc6; background: var(--green-soft); }
.flow-step.now  { border-color: #9bcac7; background: var(--primary-soft); box-shadow: var(--shadow-soft); }
```

---

## 11. 工作负荷条 (Workload Bar)

```css
.workload-row {
  display: grid;
  grid-template-columns: 60px 1fr 30px;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--muted);
}
.bar-track {
  height: 8px;
  border-radius: 999px;
  background: var(--surface-2);
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: #111827;
}
```

---

## 12. 动画规范

| 场景 | 属性 | 时长 | 缓动 |
|------|------|------|------|
| 侧边栏展开 | width | 220ms | ease |
| 文字淡入 | opacity + transform | 160ms | ease |
| 导航项 hover | background + transform | 160ms | ease |
| 卡片 hover | transform + box-shadow | 160ms | ease |
| 按钮 hover | background | 160ms | ease |
| Badge 淡入 | opacity | 160ms | ease |

---

## 13. 响应式断点

```css
@media (max-width: 1000px) {
  body { overflow: auto; }
  .sidebar {
    width: var(--sidebar-open);
    transform: translateX(calc(-1 * var(--sidebar-open) + var(--sidebar)));
  }
  .sidebar:hover, .sidebar.open { transform: translateX(0); }
  .main { margin-left: var(--sidebar); width: calc(100vw - var(--sidebar)); }
  .metrics { grid-template-columns: repeat(2, 1fr); }  /* 或 repeat(3, 1fr) */
  .content-grid { grid-template-columns: 1fr; }
}
```

---

## 14. 禁止事项

- ❌ 禁止 `border-radius: 0 !important` 全局覆盖
- ❌ 禁止 `box-shadow: none !important` 全局覆盖
- ❌ 禁止深色侧边栏背景（`#162532`、`#102426` 等）
- ❌ 禁止固定宽度侧边栏（必须可折叠）
- ❌ 禁止侧边栏用 `position: static` 或 grid 布局（必须 `position: fixed` + `margin-left`）

---

## 15. 文件结构

```
frontend/
├── packages/shared-ui/src/
│   ├── components/
│   │   └── CollapsibleSidebar.vue    ← 两端共用侧边栏
│   └── styles/
│       ├── tokens.css                ← 设计令牌
│       ├── base.css                  ← 基础重置
│       ├── components.css            ← 组件样式
│       └── layouts.css               ← 布局样式
├── apps/doctor-web/src/
│   ├── layouts/DoctorWorkspaceLayout.vue
│   └── style.css                     ← 医生端特有样式
└── apps/admin-web/src/
    ├── layouts/AdminWorkspaceLayout.vue
    └── style.css                     ← 管理端特有样式
```
