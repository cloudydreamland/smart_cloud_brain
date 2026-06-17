<script setup lang="ts">
defineProps<{ options: Array<{ label: string; value: string }>; modelValue: string }>();
defineEmits<{ "update:modelValue": [value: string] }>();
</script>

<template>
  <div class="segmented">
    <button
      v-for="item in options"
      :key="item.value"
      type="button"
      :class="{ active: item.value === modelValue }"
      @click="$emit('update:modelValue', item.value)"
    >
      {{ item.label }}
    </button>
  </div>
</template>
