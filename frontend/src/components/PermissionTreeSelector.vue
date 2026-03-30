<template>
  <div class="permission-tree-selector">
    <div class="selector-main surface-panel">
      <div class="selector-head">
        <div>
          <div class="selector-title">{{ title }}</div>
          <div class="selector-desc">{{ description }}</div>
        </div>
        <div class="selector-tools">
          <el-button size="small" @click="expandAll">全部展开</el-button>
          <el-button size="small" @click="collapseAll">全部收起</el-button>
          <el-button size="small" type="primary" plain @click="selectAll">全选</el-button>
          <el-button size="small" type="danger" plain @click="clearAll">清空</el-button>
        </div>
      </div>

      <el-tree
        ref="treeRef"
        class="tree"
        :data="treeData"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="modelValue"
        :props="{ label: 'name', children: 'children' }"
        @check="handleCheck"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <el-tag size="small" :type="tagType(data.type)">{{ data.type }}</el-tag>
            <span class="tree-name">{{ data.name }}</span>
            <span class="tree-code">{{ data.code }}</span>
          </span>
        </template>
      </el-tree>
    </div>

    <aside class="selector-side">
      <div class="side-panel surface-panel">
        <div class="side-title">当前选择</div>
        <div class="side-value">{{ checkedIds.length }}</div>
        <div class="side-muted">节点</div>
      </div>
      <div class="side-panel surface-panel">
        <div class="side-title">已选编码</div>
        <div class="side-list">
          <span v-for="item in selectedCodes" :key="item" class="side-chip">{{ item }}</span>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { PermissionNode } from '@/types/domain'

const props = defineProps<{
  modelValue: number[]
  treeData: PermissionNode[]
  title: string
  description: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: number[]): void
}>()

const treeRef = ref()
const checkedIds = ref<number[]>([])

watch(
  () => props.modelValue,
  async (value) => {
    checkedIds.value = [...value]
    await nextTick()
    treeRef.value?.setCheckedKeys(value)
  },
  { immediate: true, deep: true }
)

const selectedCodes = computed(() => {
  const map = new Map<number, string>()
  const walk = (nodes: PermissionNode[]) => {
    nodes.forEach((node) => {
      map.set(node.id, node.code)
      if (node.children?.length) walk(node.children)
    })
  }
  walk(props.treeData)
  return checkedIds.value.map((id) => map.get(id)).filter(Boolean) as string[]
})

function handleCheck(_node: PermissionNode, payload: { checkedKeys: number[] }) {
  checkedIds.value = payload.checkedKeys as number[]
  emit('update:modelValue', checkedIds.value)
}

function selectAll() {
  const ids: number[] = []
  const walk = (nodes: PermissionNode[]) => {
    nodes.forEach((node) => {
      ids.push(node.id)
      if (node.children?.length) walk(node.children)
    })
  }
  walk(props.treeData)
  checkedIds.value = ids
  emit('update:modelValue', ids)
  treeRef.value?.setCheckedKeys(ids)
}

function clearAll() {
  checkedIds.value = []
  emit('update:modelValue', [])
  treeRef.value?.setCheckedKeys([])
}

function expandAll() {
  treeRef.value?.store?.nodesMap && Object.values(treeRef.value.store.nodesMap).forEach((node: any) => (node.expanded = true))
}

function collapseAll() {
  treeRef.value?.store?.nodesMap && Object.values(treeRef.value.store.nodesMap).forEach((node: any) => (node.expanded = false))
}

function tagType(type: string) {
  if (type === 'MENU') return 'primary'
  if (type === 'BUTTON') return 'success'
  return 'warning'
}
</script>

<style scoped lang="scss">
.permission-tree-selector {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 16px;
}

.selector-main {
  padding: 18px;
}

.selector-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.selector-title {
  font-size: 18px;
  font-weight: 700;
}

.selector-desc {
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.selector-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.tree-name {
  font-weight: 500;
}

.tree-code {
  color: var(--app-text-muted);
  font-size: 12px;
}

.selector-side {
  display: grid;
  gap: 16px;
}

.side-panel {
  padding: 18px;
}

.side-title {
  color: var(--app-text-muted);
  font-size: 13px;
}

.side-value {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 800;
}

.side-muted {
  color: var(--app-text-muted);
}

.side-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.side-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: var(--app-primary-strong);
  font-size: 12px;
}

@media (max-width: 1000px) {
  .permission-tree-selector {
    grid-template-columns: 1fr;
  }
}
</style>

