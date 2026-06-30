import { inject, ref, type Ref } from "vue";
import { formatApiError } from "@smart-cloud-brain/shared-api";
import type { Toast } from "@smart-cloud-brain/shared-ui";

export interface UsePageAsyncOptions {
  /** 异步数据加载函数 */
  fetchFn: () => Promise<void>;
  /** 成功后的 Toast 消息（可选） */
  successMessage?: string;
  /** 失败后的错误前缀（默认 "操作失败"） */
  errorMessage?: string;
  /** 是否在成功后显示 Toast（默认 true） */
  showToast?: boolean;
}

export interface UsePageAsyncReturn {
  loading: Ref<boolean>;
  loaded: Ref<boolean>;
  error: Ref<string>;
  refresh: () => Promise<void>;
}

/**
 * 页面级异步操作管理 composable。
 *
 * 提供标准的 loading / loaded / error 三件套和 refresh 函数，
 * 内置错误处理和 Toast 反馈。
 *
 * 用法：
 * ```ts
 * const { loading, loaded, error, refresh } = usePageAsync({
 *   fetchFn: async () => {
 *     const data = await api.someEndpoint();
 *     items.value = data;
 *   },
 *   successMessage: "数据已刷新",
 *   errorMessage: "数据加载失败",
 * });
 * ```
 */
export function usePageAsync(options: UsePageAsyncOptions): UsePageAsyncReturn {
  const loading = ref(false);
  const loaded = ref(false);
  const error = ref("");

  let toast: Ref<InstanceType<typeof Toast>> | null = null;
  try {
    toast = inject<Ref<InstanceType<typeof Toast>>>("toast") ?? null;
  } catch {
    toast = null;
  }

  async function refresh() {
    loading.value = true;
    error.value = "";
    try {
      await options.fetchFn();
      loaded.value = true;
      if (options.showToast !== false && options.successMessage) {
        toast?.value?.success("操作成功", options.successMessage);
      }
    } catch (err) {
      error.value = formatApiError(err, options.errorMessage || "操作失败");
      toast?.value?.error("操作失败", "请检查网络后重试。");
    } finally {
      loading.value = false;
    }
  }

  return { loading, loaded, error, refresh };
}
