<script setup lang="ts">
defineProps<{ open: boolean; title: string; description?: string }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="drawer-backdrop" role="presentation" @click.self="$emit('close')">
      <aside class="drawer-card" role="dialog" aria-modal="true" :aria-label="title">
        <header class="panel-header">
          <div class="panel-title">
            <h2>{{ title }}</h2>
            <p v-if="description">{{ description }}</p>
          </div>
          <button type="button" class="ghost" aria-label="关闭抽屉" @click="$emit('close')">关闭</button>
        </header>
        <div class="drawer-body">
          <slot />
        </div>
      </aside>
    </div>
  </Teleport>
</template>
