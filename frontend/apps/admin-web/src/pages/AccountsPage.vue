<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  displayText,
  formatApiError,
  statusClass,
  toNumber,
  useAdminWorkflowStore,
  usePagination,
  type AccountSaveRequest,
  type Account,
  type Role,
  type RoleInfo,
} from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, FormField, Modal, PaginationBar, ScbSelect, SegmentedControl, StatusTag } from "@smart-cloud-brain/shared-ui";

const workflow = useAdminWorkflowStore();
const { departments } = storeToRefs(workflow);
const accounts = ref<Account[]>([]);
const roles = ref<RoleInfo[]>([]);
const activeRole = ref("ADMIN");
const keyword = ref("");
const error = ref("");
const notice = ref("");
const loading = ref(false);
const saving = ref(false);
const editorOpen = ref(false);
const form = reactive<{
  id?: number;
  role: Role;
  account: string;
  name: string;
  password: string;
  departmentId: number;
  title: string;
  specialty: string;
  status: string;
}>({
  role: "ADMIN",
  account: "",
  name: "",
  password: "",
  departmentId: 0,
  title: "",
  specialty: "",
  status: "ENABLED",
});

const fallbackRoleOptions = [
  { value: "ADMIN", label: "系统管理员" },
  { value: "DOCTOR", label: "医生" },
];

const roleOptions = computed(() => {
  const seen = new Set<string>();
  const options = [...roles.value, ...accounts.value]
    .map((item) => {
      const value = displayText(item.role, "");
      if (!value || seen.has(value)) return null;
      seen.add(value);
      return { value, label: displayText(item.label, roleLabel(value)) };
    })
    .filter((item): item is { value: string; label: string } => Boolean(item));
  const source = options.length ? options : fallbackRoleOptions;
  return source.map((item) => ({
    ...item,
    label: `${item.label} ${accountsByRole(item.value).length}`,
  }));
});

const accountRoleFormOptions = [
  { value: "ADMIN", label: "系统管理员" },
  { value: "DOCTOR", label: "医生" },
];
const accountDeptOptions = computed(() => [
  { value: 0, label: "请选择科室" },
  ...departments.value.map((d) => ({ value: toNumber(d.id), label: displayText(d.name) })),
]);
const accountStatusOptions = [
  { value: "ENABLED", label: "启用" },
  { value: "DISABLED", label: "停用" },
];

const currentRoleLabel = computed(() => roleLabel(activeRole.value));
const currentRoleTemplate = computed(() => roles.value.find((item) => item.role === activeRole.value));
const currentRoleTotal = computed(() => accountsByRole(activeRole.value).length);
const currentRoleEnabled = computed(() => accountsByRole(activeRole.value).filter((item) => displayText(item.status, "ENABLED") !== "DISABLED").length);

const currentRoleAccounts = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  const source = accountsByRole(activeRole.value);
  if (!q) return source;
  return source.filter((item) => [
    item.roleLabel, item.role, item.account, item.name, item.departmentName, item.title, item.status, item.permissions,
  ].some((value) => displayText(value, "").toLowerCase().includes(q)));
});
const { currentPage, pageSize, total, pageRows } = usePagination(currentRoleAccounts, 8);

watch([activeRole, keyword], () => {
  currentPage.value = 1;
});

watch(roleOptions, (options) => {
  if (!options.some((item) => item.value === activeRole.value)) {
    activeRole.value = options[0]?.value ?? "ADMIN";
  }
}, { immediate: true });

function accountsByRole(role: string) {
  return accounts.value.filter((item) => item.role === role);
}

function roleLabel(role: unknown) {
  const raw = String(role || "");
  return displayText(roles.value.find((item) => item.role === raw)?.label, fallbackRoleOptions.find((item) => item.value === raw)?.label ?? raw);
}

function rolePermissionText(role: unknown) {
  const raw = String(role || "");
  return displayText(roles.value.find((item) => item.role === raw)?.permissions, "");
}

function defaultDepartmentId() {
  return toNumber(departments.value[0]?.id, 0);
}

function defaultCreateRole(): Role {
  return activeRole.value === "DOCTOR" ? "DOCTOR" : "ADMIN";
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    const [accountList, roleList] = await Promise.all([
      api.accounts(),
      api.roles(),
    ]);
    accounts.value = accountList as Account[];
    roles.value = roleList as RoleInfo[];
    if (!departments.value.length) {
      await workflow.refresh();
    }
  } catch (err) {
    error.value = formatApiError(err, "账户权限数据加载失败");
  } finally {
    loading.value = false;
  }
}

function openEditor(item?: Account) {
  error.value = "";
  notice.value = "";
  form.id = item ? toNumber(item.id, undefined) : undefined;
  form.role = item ? ((displayText(item.role, "ADMIN") as Role) || "ADMIN") : defaultCreateRole();
  form.account = displayText(item?.account);
  form.name = displayText(item?.name);
  form.password = "";
  form.departmentId = toNumber(item?.departmentId, form.role === "DOCTOR" ? defaultDepartmentId() : 0);
  form.title = displayText(item?.title);
  form.specialty = displayText(item?.specialty);
  form.status = displayText(item?.status, "ENABLED");
  if (!item && form.role === "DOCTOR" && !form.departmentId) {
    form.departmentId = defaultDepartmentId();
  }
  editorOpen.value = true;
}

function onRoleChange() {
  if (form.role === "ADMIN") {
    form.departmentId = 0;
    form.title = "";
    form.specialty = "";
  } else if (!form.departmentId) {
    form.departmentId = defaultDepartmentId();
  }
}

function validateForm() {
  form.account = form.account.trim();
  form.name = form.name.trim();
  if (!form.role) return "请选择账户角色";
  if (!form.account) return form.role === "ADMIN" ? "请填写管理员账号" : "请填写医生手机号";
  if (!form.name) return "请填写姓名";
  if (!form.id && !form.password.trim()) return "新增账户必须设置初始密码";
  if (form.password && form.password.trim().length < 6) return "密码至少 6 位";
  if (form.role === "DOCTOR") {
    if (!/^\d{11}$/.test(form.account)) return "医生账号必须为 11 位手机号";
    if (!form.departmentId) return "请选择医生所属科室";
  }
  return "";
}

function saveBody(): AccountSaveRequest {
  return {
    id: form.id,
    role: form.role,
    account: form.account,
    name: form.name,
    password: form.password.trim() || undefined,
    departmentId: form.role === "DOCTOR" ? form.departmentId : undefined,
    title: form.role === "DOCTOR" ? form.title : undefined,
    specialty: form.role === "DOCTOR" ? form.specialty : undefined,
    status: form.status || "ENABLED",
  };
}

async function save() {
  const invalid = validateForm();
  if (invalid) {
    error.value = invalid;
    return;
  }
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.saveAccount(saveBody());
    editorOpen.value = false;
    notice.value = "账户与权限已保存。";
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "账户保存失败");
  } finally {
    saving.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <section class="account-layout">
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title">
          <h2>账户与权限管理</h2>
        </div>
        <div class="toolbar">
          <button type="button" :disabled="loading" @click="refresh">刷新</button>
          <button type="button" class="primary" @click="openEditor()">新增账户</button>
        </div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="account-role-bar">
          <SegmentedControl v-model="activeRole" :options="roleOptions" />
          <div class="account-role-summary">
            <strong>{{ currentRoleLabel }}</strong>
            <span>{{ currentRoleTotal }} 个账户</span>
            <span>{{ currentRoleEnabled }} 个启用</span>
          </div>
        </div>
        <div class="admin-filter-row">
          <input v-model.trim="keyword" :placeholder="`搜索${currentRoleLabel}账号、姓名或权限`" />
        </div>
        <DataTable :rows="currentRoleAccounts" :loading="loading" :error="error" :breakout="true" :empty-title="`${currentRoleLabel}暂无账户`" empty-message="当前角色下还没有可管理账户。">
          <thead>
            <tr>
              <th>账号</th>
              <th>姓名</th>
              <th>所属科室</th>
              <th>状态</th>
              <th>访问权限</th>
              <th class="actions-cell">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pageRows" :key="`${displayText(item.role)}-${displayText(item.id)}`">
              <td>{{ displayText(item.account) }}</td>
              <td>{{ displayText(item.name) }}</td>
              <td>{{ displayText(item.departmentName) }}</td>
              <td><StatusTag :status="displayText(item.status)" :tone="statusClass(item.status)" /></td>
              <td class="permission-cell">{{ displayText(item.permissions) }}</td>
              <td><button type="button" class="action-btn" @click="openEditor(item)">编辑</button></td>
            </tr>
          </tbody>
        </DataTable>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
    </section>

    <aside class="panel">
      <header class="panel-header">
        <div class="panel-title">
          <h2>当前角色权限</h2>
        </div>
      </header>
      <div class="panel-body role-template-list">
        <article class="role-template role-template-active">
          <strong>{{ currentRoleLabel }}</strong>
          <span class="tag">{{ activeRole }}</span>
          <p>{{ displayText(currentRoleTemplate?.permissions, "暂无权限说明") }}</p>
        </article>
        <div class="role-switch-list">
          <button
            v-for="role in roleOptions"
            :key="role.value"
            type="button"
            class="role-switch-card"
            :class="{ active: role.value === activeRole }"
            @click="activeRole = role.value"
          >
            <span>{{ roleLabel(role.value) }}</span>
            <strong>{{ accountsByRole(role.value).length }}</strong>
          </button>
        </div>
      </div>
    </aside>

    <Modal :open="editorOpen" title="账户与权限" description="选择角色并维护登录账号、状态和访问范围。" @close="editorOpen = false">
      <div class="stack">
        <FormField label="角色">
          <ScbSelect v-model="form.role" :options="accountRoleFormOptions" :disabled="Boolean(form.id)" @update:modelValue="onRoleChange" />
        </FormField>
        <FormField :label="form.role === 'ADMIN' ? '管理员账号' : '医生手机号'">
          <input v-model="form.account" :placeholder="form.role === 'ADMIN' ? 'admin2' : '13800000002'" />
        </FormField>
        <FormField label="姓名">
          <input v-model="form.name" />
        </FormField>
        <FormField :label="form.id ? '重置密码' : '初始密码'" :hint="form.id ? '留空则不修改密码' : '至少 6 位'">
          <input v-model="form.password" type="password" autocomplete="new-password" />
        </FormField>
        <FormField v-if="form.role === 'DOCTOR'" label="所属科室">
          <ScbSelect v-model="form.departmentId" :options="accountDeptOptions" />
        </FormField>
        <FormField v-if="form.role === 'DOCTOR'" label="职称">
          <input v-model="form.title" />
        </FormField>
        <FormField v-if="form.role === 'DOCTOR'" label="专长">
          <textarea v-model="form.specialty" />
        </FormField>
        <FormField label="状态">
          <ScbSelect v-model="form.status" :options="accountStatusOptions" />
        </FormField>
        <div class="notice warning">{{ rolePermissionText(form.role) }}</div>
      </div>
      <template #footer>
        <button type="button" :disabled="saving" @click="editorOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="save">{{ saving ? "保存中..." : "保存" }}</button>
      </template>
    </Modal>
  </section>
</template>
