<template>
  <header class="sticky top-0 z-50 bg-white border-b border-[var(--line)] backdrop-blur-md h-14">
    <div class="max-w-[1200px] mx-auto px-4 h-full flex items-center justify-between">
      <!-- Left: Logo + Menu -->
      <div class="flex items-center gap-8">
        <!-- Logo -->
        <router-link to="/patient/home" class="flex items-center gap-2 no-underline text-[var(--ink)] shrink-0">
          <div class="w-7 h-7 flex items-center justify-center text-base bg-[var(--primary-soft)] rounded-lg">
            🧠
          </div>
          <span class="text-[15px] font-bold tracking-tight">智慧云脑</span>
        </router-link>

        <!-- Desktop nav -->
        <nav class="hidden md:flex items-center gap-0.5">
          <router-link
            v-for="item in navItems"
            :key="item.value"
            :to="item.to"
            :class="[
              'inline-flex items-center px-3.5 py-1.5 text-[14px] font-medium rounded-md transition-colors no-underline',
              isActive(item.value)
                ? 'text-[var(--ink)] font-semibold'
                : 'text-[var(--muted)] hover:bg-[var(--surface-alt)] hover:text-[var(--ink)]',
            ]"
          >
            {{ item.label }}
          </router-link>
        </nav>
      </div>

      <!-- Right: Actions -->
      <div class="flex items-center gap-2">
        <!-- User menu (desktop) -->
        <div class="hidden md:flex items-center gap-2">
          <router-link
            to="/patient/profile"
            class="inline-flex items-center gap-2 px-3 py-1.5 text-sm text-[var(--muted)] hover:bg-[var(--surface-alt)] rounded-md transition-colors no-underline"
          >
            <div class="h-7 w-7 rounded-full bg-[var(--primary-soft)] flex items-center justify-center text-xs font-semibold text-[var(--primary)]">
              {{ userName?.charAt(0) || 'U' }}
            </div>
            <span class="text-sm font-medium">{{ userName || '患者' }}</span>
          </router-link>
          <button
            class="text-sm text-[var(--muted)] hover:text-[var(--danger)] transition-colors px-2 py-1"
            @click="$emit('logout')"
          >
            退出
          </button>
        </div>

        <!-- Mobile menu toggle -->
        <button
          class="md:hidden flex items-center justify-center h-9 w-9 border border-[var(--line)] rounded-md bg-white text-[var(--ink)]"
          @click="mobileOpen = true"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <line x1="4" x2="20" y1="12" y2="12"/><line x1="4" x2="20" y1="6" y2="6"/><line x1="4" x2="20" y1="18" y2="18"/>
          </svg>
        </button>
      </div>
    </div>
  </header>

  <!-- Mobile sheet -->
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="mobileOpen"
        class="fixed inset-0 z-[100] bg-black/50 backdrop-blur-sm md:hidden"
        @click="mobileOpen = false"
      />
    </Transition>
    <Transition
      enter-active-class="transition-transform duration-200 ease-out"
      enter-from-class="-translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition-transform duration-150 ease-in"
      leave-from-class="translate-x-0"
      leave-to-class="-translate-x-full"
    >
      <div
        v-if="mobileOpen"
        class="fixed top-0 left-0 z-[101] w-[280px] h-full bg-white border-r border-[var(--line)] shadow-lg flex flex-col md:hidden"
      >
        <!-- Sheet header -->
        <div class="flex items-center justify-between p-4 border-b border-[var(--line)]">
          <span class="text-sm font-semibold">菜单</span>
          <button
            class="flex items-center justify-center h-7 w-7 rounded-md text-[var(--muted)] hover:bg-[var(--surface-alt)]"
            @click="mobileOpen = false"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <!-- Sheet nav -->
        <nav class="flex flex-col gap-0.5 p-2 flex-1">
          <router-link
            v-for="item in navItems"
            :key="item.value"
            :to="item.to"
            :class="[
              'flex items-center gap-2 px-3 py-2.5 text-[15px] font-medium rounded-md transition-colors no-underline',
              isActive(item.value)
                ? 'bg-[var(--primary)] text-white'
                : 'text-[var(--muted)] hover:bg-[var(--surface-alt)]',
            ]"
            @click="mobileOpen = false"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <!-- Sheet footer -->
        <div class="p-4 border-t border-[var(--line)]">
          <button
            class="w-full text-sm text-[var(--danger)] hover:bg-[var(--danger-soft)] py-2 rounded-md transition-colors"
            @click="$emit('logout'); mobileOpen = false"
          >
            退出登录
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'

interface NavItem {
  label: string
  value: string
  to: string
}

interface Props {
  userName?: string
  navItems?: NavItem[]
}

const props = withDefaults(defineProps<Props>(), {
  userName: '',
  navItems: () => [
    { label: '首页', value: 'home', to: '/patient/home' },
    { label: '智能分诊', value: 'triage', to: '/patient/triage' },
    { label: '在线挂号', value: 'booking', to: '/patient/booking' },
    { label: '我的病历', value: 'records', to: '/patient/records' },
    { label: '我的处方', value: 'prescriptions', to: '/patient/prescriptions' },
  ],
})

defineEmits<{ logout: [] }>()

const route = useRoute()
const mobileOpen = ref(false)

function isActive(value: string) {
  return route.path.toLowerCase().includes(`/${value}`)
}
</script>
