<template>
  <div class="settings-page">
    <section class="hero surface-panel">
      <div>
        <div class="hero-kicker">系统设置</div>
        <h1>基础设置</h1>
        <p>基础品牌信息、登录提示和维护状态已经接入后端，可直接保存到系统配置表。</p>
      </div>
      <el-tag type="success">后端已接入</el-tag>
    </section>

    <section class="content-grid">
      <div class="surface-panel form-panel">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="settings-form">
          <el-form-item label="系统名称" prop="systemName">
            <el-input v-model="form.systemName" />
          </el-form-item>
          <el-form-item label="系统副标题" prop="systemSubtitle">
            <el-input v-model="form.systemSubtitle" />
          </el-form-item>
          <el-form-item label="公司名称" prop="companyName">
            <el-input v-model="form.companyName" />
          </el-form-item>
          <el-form-item label="支持邮箱" prop="supportEmail">
            <el-input v-model="form.supportEmail" />
          </el-form-item>
          <el-form-item label="支持电话" prop="supportPhone">
            <el-input v-model="form.supportPhone" />
          </el-form-item>
          <el-form-item label="登录提示" prop="loginNotice">
            <el-input v-model="form.loginNotice" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item label="维护模式">
            <el-switch v-model="form.maintenanceMode" inline-prompt active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
            <el-button @click="reset">恢复默认</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="surface-panel preview-panel">
        <div class="panel-head">
          <h2>登录页预览</h2>
          <p>这部分会反映系统文案、支持信息和维护状态。</p>
        </div>
        <div class="preview-card">
          <div class="preview-title">{{ form.systemName }}</div>
          <div class="preview-subtitle">{{ form.systemSubtitle }}</div>
          <div class="preview-line">{{ form.companyName }}</div>
          <div class="preview-line">支持邮箱：{{ form.supportEmail || '-' }}</div>
          <div class="preview-line">支持电话：{{ form.supportPhone || '-' }}</div>
          <div class="preview-line">维护模式：{{ form.maintenanceMode ? '开启' : '关闭' }}</div>
          <div class="preview-notice">{{ form.loginNotice }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getBasicSettings, saveBasicSettings } from '@/api/settings'
import { defaultBasicSettings, loadBasicSettings, saveBasicSettings as persistBasicSettings } from '@/utils/settings'
import type { BasicSettingsState } from '@/types/domain'

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive<BasicSettingsState>(loadBasicSettings())

const rules: FormRules<BasicSettingsState> = {
  systemName: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
  systemSubtitle: [{ required: true, message: '请输入系统副标题', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  supportEmail: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  loginNotice: [{ required: true, message: '请输入登录提示', trigger: 'blur' }]
}

async function loadRemote() {
  try {
    const res = await getBasicSettings()
    if (res.code === 200 && res.data) {
      Object.assign(form, defaultBasicSettings, res.data)
      persistBasicSettings({ ...form })
      return
    }
  } catch {
    // fall back to local cache below
  }
  Object.assign(form, loadBasicSettings())
}

async function save() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const res = await saveBasicSettings(form)
    if (res.code === 200 && res.data) {
      Object.assign(form, defaultBasicSettings, res.data)
      persistBasicSettings({ ...form })
      ElMessage.success('基础设置已保存')
      return
    }
    throw new Error(res.message || '保存基础设置失败')
  } catch (error) {
    ElMessage.error((error as Error).message || '保存基础设置失败')
  } finally {
    saving.value = false
  }
}

function reset() {
  Object.assign(form, defaultBasicSettings)
  persistBasicSettings({ ...form })
}

onMounted(() => {
  void loadRemote()
})
</script>

<style scoped lang="scss">
.settings-page { display: grid; gap: 16px; }
.hero { padding: 20px; display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.hero-kicker { font-size: 12px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--app-text-muted); }
.hero h1, .hero p { margin: 0; }
.hero p { margin-top: 8px; color: var(--app-text-muted); }
.content-grid { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr); gap: 16px; }
.form-panel, .preview-panel { padding: 20px; }
.settings-form { max-width: 760px; }
.panel-head h2, .panel-head p { margin: 0; }
.panel-head p { margin-top: 6px; color: var(--app-text-muted); }
.preview-card { padding: 18px; border-radius: 18px; background: rgba(59, 130, 246, 0.06); display: grid; gap: 10px; }
.preview-title { font-size: 26px; font-weight: 800; }
.preview-subtitle { font-size: 15px; color: var(--app-text-muted); }
.preview-line, .preview-notice { color: var(--app-text-muted); line-height: 1.6; }
@media (max-width: 960px) { .hero, .content-grid { grid-template-columns: 1fr; } .hero { flex-direction: column; } }
</style>
