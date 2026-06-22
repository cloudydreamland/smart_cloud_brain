<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  fieldText,
  formatApiError,
  statusClass,
  toNumber,
  useAdminWorkflowStore,
  useAuthStore,
  usePagination,
  type AccountSaveRequest,
  type DataRow,
  type Role,
} from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, FormField, Modal, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { departments } = storeToRefs(workflow);
const accounts = ref<DataRow[]>([]);
const roles = ref<DataRow[]>([]);
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

const filteredAccounts = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  if (!q) return accounts.value;
  return accounts.value.filter((item) => [
    "roleLabel", "role", "account", "name", "departmentName", "title", "status", "permissions",
  ].some((key) => fieldText(item, key, "").toLowerCase().includes(q)));
});
const { currentPage, pageSize, total, pageRows } = usePagination(filteredAccounts, 8);

function roleLabel(role: unknown) {
  const raw = String(role || "");
  return fieldText(roles.value.find((item) => fieldText(item, "role") === raw), "label", raw);
}

function rolePermissionText(role: unknown) {
  const raw = String(role || "");
  return fieldText(roles.value.find((item) => fieldText(item, "role") === raw), "permissions", "");
}

function defaultDepartmentId() {
  return toNumber(departments.value[0]?.id, 0);
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    const [accountList, roleList] = await Promise.all([
      api.accounts(auth.token()),
      api.roles(auth.token()),
    ]);
    accounts.value = accountList;
    roles.value = roleList;
    if (!departments.value.length) {
      await workflow.refresh(auth.token());
    }
  } catch (err) {
    error.value = formatApiError(err, "账户权限数据加载失败");
  } finally {
    loading.value = false;
  }
}

function openEditor(item?: DataRow) {
  error.value = "";
  notice.value = "";
  form.id = item ? toNumber(item.id, undefined) : undefined;
  form.role = (fieldText(item, "role", "ADMIN") as Role) || "ADMIN";
  form.account = fieldText(item, "account");
  form.name = fieldText(item, "name");
  form.password = "";
  form.departmentId = toNumber(item?.departmentId, form.role === "DOCTOR" ? defaultDepartmentId() : 0);
  form.title = fieldText(item, "title");
  form.specialty = fieldText(item, "specialty");
  form.status = fieldText(item, "status", "ENABLED");
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
    await api.saveAccount(auth.token(), saveBody());
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
          <p class="eyebrow">访问控制</p>
          <h2>账户与权限管理</h2>
          <p>统一维护管理员和医生账号，按角色分配系统访问范围。</p>
        </div>
        <div class="toolbar">
          <button type="button" :disabled="loading" @click="refresh">刷新</button>
          <button type="button" class="primary" @click="openEditor()">新增账户</button>
        </div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="admin-filter-row">
          <input v-model.trim="keyword" placeholder="搜索账号、姓名、角色或权限" />
        </div>
        <DataTable :rows="filteredAccounts" :loading="loading" :error="error" empty-title="暂无账户" empty-message="还没有可管理的管理员或医生账户。">
          <thead>
            <tr>
              <th>角色</th>
              <th>账号</th>
              <th>姓名</th>
              <th>所属科室</th>
              <th>状态</th>
              <th>访问权限</th>
              <th class="actions-cell">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pageRows" :key="`${fieldText(item, 'role')}-${fieldText(item, 'id')}`">
              <td><strong>{{ fieldText(item, "roleLabel", roleLabel(item.role)) }}</strong></td>
              <td>{{ fieldText(item, "account") }}</td>
              <td>{{ fieldText(item, "name") }}</td>
              <td>{{ fieldText(item, "departmentName", "-") }}</td>
              <td><StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" /></td>
              <td class="permission-cell">{{ fieldText(item, "permissions") }}</td>
              <td><button type="button" @click="openEditor(item)">编辑</button></td>
            </tr>
          </tbody>
        </DataTable>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
    </section>

    <aside class="panel">
      <header class="panel-header">
        <div class="panel-title">
          <h2>角色权限模板</h2>
          <p>系统访问控制由登录角色决定，保存账号后立即按角色生效。</p>
        </div>
      </header>
      <div class="panel-body role-template-list">
        <article v-for="role in roles" :key="fieldText(role, 'role')" class="role-template">
          <strong>{{ fieldText(role, "label") }}</strong>
          <span class="tag">{{ fieldText(role, "role") }}</span>
          <p>{{ fieldText(role, "permissions") }}</p>
        </article>
      </div>
    </aside>

    <Modal :open="editorOpen" title="账户与权限" description="选择角色并维护登录账号、状态和访问范围。" @close="editorOpen = false">
      <div class="stack">
        <FormField label="角色">
          <select v-model="form.role" :disabled="Boolean(form.id)" @change="onRoleChange">
            <option value="ADMIN">系统管理员</option>
            <option value="DOCTOR">医生</option>
          </select>
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
          <select v-model.number="form.departmentId">
            <option :value="0" disabled>请选择科室</option>
            <option v-for="department in departments" :key="String(department.id)" :value="toNumber(department.id)">
              {{ fieldText(department, "name") }}
            </option>
          </select>
        </FormField>
        <FormField v-if="form.role === 'DOCTOR'" label="职称">
          <input v-model="form.title" />
        </FormField>
        <FormField v-if="form.role === 'DOCTOR'" label="专长">
          <textarea v-model="form.specialty" />
        </FormField>
        <FormField label="状态">
          <select v-model="form.status">
            <option value="ENABLED">启用</option>
            <option value="DISABLED">停用</option>
          </select>
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
