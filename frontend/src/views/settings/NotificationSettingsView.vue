<template>
  <div class="settings-page">
    <section class="hero surface-panel">
      <div>
        <div class="hero-kicker">系统设置</div>
        <h1>通知设置</h1>
        <p>邮件通道、Webhook 和核心告警项都已经接入后端配置，保存后可直接长期生效。</p>
      </div>
      <el-tag type="success">后端已接入</el-tag>
    </section>

    <section class="content-grid">
      <div class="surface-panel form-panel">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="128px" class="settings-form">
          <el-form-item label="邮件通知">
            <el-switch v-model="form.emailEnabled" inline-prompt active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <el-form-item label="SMTP 主机" prop="emailHost">
            <el-input v-model="form.emailHost" placeholder="smtp.example.com" />
          </el-form-item>
          <el-form-item label="SMTP 端口" prop="emailPort">
            <el-input-number v-model="form.emailPort" :min="1" :max="65535" style="width: 100%" />
          </el-form-item>
          <el-form-item label="SMTP 用户名" prop="emailUsername">
            <el-input v-model="form.emailUsername" />
          </el-form-item>
          <el-form-item label="发件地址" prop="emailFrom">
            <el-input v-model="form.emailFrom" placeholder="noreply@example.com" />
          </el-form-item>
          <el-form-item label="Webhook">
            <el-switch v-model="form.webhookEnabled" inline-prompt active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <el-form-item label="Webhook 地址" prop="webhookUrl">
            <el-input v-model="form.webhookUrl" placeholder="https://..." />
          </el-form-item>
          <el-form-item label="任务失败告警">
            <el-switch v-model="form.taskFailureAlert" inline-prompt active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <el-form-item label="机器人离线告警">
            <el-switch v-model="form.robotOfflineAlert" inline-prompt active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存通知设置</el-button>
            <el-button @click="reset">恢复默认</el-button>
            <el-button plain @click="testNotify">发送测试通知</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="surface-panel preview-panel">
        <div class="panel-head">
          <h2>通知概览</h2>
          <p>这里可以快速确认哪些渠道已开启，避免值班时误配。</p>
        </div>
        <div class="preview-card">
          <div class="preview-item">邮件通知：{{ form.emailEnabled ? '开启' : '关闭' }}</div>
          <div class="preview-item">SMTP：{{ form.emailHost || '-' }}:{{ form.emailPort }}</div>
          <div class="preview-item">SMTP 用户：{{ form.emailUsername || '-' }}</div>
          <div class="preview-item">发件地址：{{ form.emailFrom || '-' }}</div>
          <div class="preview-item">Webhook：{{ form.webhookEnabled ? '开启' : '关闭' }}</div>
          <div class="preview-item">Webhook 地址：{{ form.webhookUrl || '-' }}</div>
          <div class="preview-item">任务失败告警：{{ form.taskFailureAlert ? '开启' : '关闭' }}</div>
          <div class="preview-item">机器人离线告警：{{ form.robotOfflineAlert ? '开启' : '关闭' }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getNotificationSettings, saveNotificationSettings } from '@/api/settings'
import { defaultNotificationSettings, loadNotificationSettings, saveNotificationSettings as persistNotificationSettings } from '@/utils/settings'
import type { NotificationSettingsState } from '@/types/domain'

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive<NotificationSettingsState>(loadNotificationSettings())

const rules: FormRules<NotificationSettingsState> = {
  emailFrom: [{ type: 'email', message: '请输入正确的发件邮箱', trigger: 'blur' }]
}

async function loadRemote() {
  try {
    const res = await getNotificationSettings()
    if (res.code === 200 && res.data) {
      Object.assign(form, defaultNotificationSettings, res.data)
      persistNotificationSettings({ ...form })
      return
    }
  } catch {
    // fall back to local cache below
  }
  Object.assign(form, loadNotificationSettings())
}

async function save() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const res = await saveNotificationSettings(form)
    if (res.code === 200 && res.data) {
      Object.assign(form, defaultNotificationSettings, res.data)
      persistNotificationSettings({ ...form })
      ElMessage.success('通知设置已保存')
      return
    }
    throw new Error(res.message || '保存通知设置失败')
  } catch (error) {
    ElMessage.error((error as Error).message || '保存通知设置失败')
  } finally {
    saving.value = false
  }
}

function reset() {
  Object.assign(form, defaultNotificationSettings)
  persistNotificationSettings({ ...form })
}

function testNotify() {
  ElMessage.success('测试通知已按当前配置模拟发送')
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
.preview-card { padding: 18px; border-radius: 18px; background: rgba(245, 158, 11, 0.08); display: grid; gap: 10px; }
.preview-item { color: var(--app-text); line-height: 1.6; }
@media (max-width: 960px) { .hero, .content-grid { grid-template-columns: 1fr; } .hero { flex-direction: column; } }
</style>
