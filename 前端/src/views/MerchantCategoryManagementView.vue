<script setup lang="ts">
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import {
  createMerchantCategory,
  deleteMerchantCategory,
  fetchMerchantCategoryList,
  updateMerchantCategory,
} from '../api/admin'
import type { MerchantCategoryForm, MerchantCategoryItem } from '../types'

const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const list = ref<MerchantCategoryItem[]>([])

const form = reactive<MerchantCategoryForm>({
  name: '',
  sort: 0,
  status: 1,
})

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    sort: 0,
    status: 1,
  })
}

async function loadCategories() {
  loading.value = true
  try {
    list.value = await fetchMerchantCategoryList()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: MerchantCategoryItem) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateMerchantCategory(form)
      ElMessage.success('分类更新成功')
    } else {
      await createMerchantCategory(form)
      ElMessage.success('分类创建成功')
    }
    dialogVisible.value = false
    await loadCategories()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: MerchantCategoryItem) {
  await ElMessageBox.confirm(`确认删除分类“${row.name}”吗？`, '删除分类', { type: 'warning' })
  await deleteMerchantCategory(row.id)
  ElMessage.success('分类删除成功')
  await loadCategories()
}

onMounted(() => {
  void loadCategories()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <div>
          <h2 class="page-title">商户分类管理</h2>
          <p class="page-subtitle">支持分类列表、新增、编辑和删除。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
      </div>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="分类 ID" min-width="100" />
        <el-table-column prop="name" label="分类名称" min-width="180" />
        <el-table-column prop="sort" label="排序值" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="520px">
      <el-form label-position="top">
        <el-form-item label="分类名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sort" :min="0" class="full-width" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="full-width">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.full-width {
  width: 100%;
}
</style>
