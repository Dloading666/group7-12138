<template>
  <div class="notification-settings">
    <el-card>
      <template #header>
        <span>通知设置</span>
      </template>

      <el-form :model="notificationForm" label-width="140px" style="max-width: 800px;">
        <el-divider content-position="left">邮件通知</el-divider>

        <el-form-item label="启用邮件通知">
          <el-switch v-model="notificationForm.email.enabled" />
        </el-form-item>

        <template v-if="notificationForm.email.enabled">
          <el-form-item label="SMTP服务器">
            <el-input v-model="notificationForm.email.smtpServer" placeholder="smtp.example.com" />
          </el-form-item>

          <el-form-item label="SMTP端口">
            <el-input-number v-model="notificationForm.email.smtpPort" :min="1" :max="65535" />
          </el-form-item>

          <el-form-item label="发件邮箱">
            <el-input v-model="notificationForm.email.sender" placeholder="noreply@example.com" />
          </el-form-item>

          <el-form-item label="邮箱密码">
            <el-input v-model="notificationForm.email.password" type="password" show-password />
          </el-form-item>

          <el-form-item label="通知场景">
            <el-checkbox-group v-model="notificationForm.email.scenes">
              <el-checkbox value="task_success">任务成功</el-checkbox>
              <el-checkbox value="task_failed">任务失败</el-checkbox>
              <el-checkbox value="robot_offline">机器人离线</el-checkbox>
              <el-checkbox value="system_alert">系统告警</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>

        <el-divider content-position="left">短信通知</el-divider>

        <el-form-item label="启用短信通知">
          <el-switch v-model="notificationForm.sms.enabled" />
        </el-form-item>

        <template v-if="notificationForm.sms.enabled">
          <el-form-item label="短信服务提供商">
            <el-select v-model="notificationForm.sms.provider" style="width: 100%;">
              <el-option label="阿里云" value="aliyun" />
              <el-option label="腾讯云" value="tencent" />
            </el-select>
          </el-form-item>

          <el-form-item label="Access Key">
            <el-input v-model="notificationForm.sms.accessKey" />
          </el-form-item>

          <el-form-item label="Access Secret">
            <el-input v-model="notificationForm.sms.accessSecret" type="password" show-password />
          </el-form-item>

          <el-form-item label="通知场景">
            <el-checkbox-group v-model="notificationForm.sms.scenes">
              <el-checkbox value="task_failed">任务失败</el-checkbox>
              <el-checkbox value="system_alert">系统告警</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>

        <el-divider content-position="left">Webhook通知</el-divider>

        <el-form-item label="启用Webhook">
          <el-switch v-model="notificationForm.webhook.enabled" />
        </el-form-item>

        <template v-if="notificationForm.webhook.enabled">
          <el-form-item label="Webhook URL">
            <el-input v-model="notificationForm.webhook.url" placeholder="https://example.com/webhook" />
          </el-form-item>

          <el-form-item label="请求方式">
            <el-select v-model="notificationForm.webhook.method" style="width: 100%;">
              <el-option label="POST" value="POST" />
              <el-option label="GET" value="GET" />
            </el-select>
          </el-form-item>

          <el-form-item label="请求头">
            <el-input
              v-model="notificationForm.webhook.headers"
              type="textarea"
              :rows="3"
              placeholder='{"Content-Type": "application/json"}'
            />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" @click="handleSave">保存设置</el-button>
          <el-button @click="handleTest">发送测试通知</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

const notificationForm = reactive({
  email: {
    enabled: true,
    smtpServer: 'smtp.example.com',
    smtpPort: 587,
    sender: 'noreply@example.com',
    password: '',
    scenes: ['task_failed', 'system_alert']
  },
  sms: {
    enabled: false,
    provider: 'aliyun',
    accessKey: '',
    accessSecret: '',
    scenes: ['task_failed', 'system_alert']
  },
  webhook: {
    enabled: false,
    url: '',
    method: 'POST',
    headers: '{"Content-Type": "application/json"}'
  }
})

const handleSave = () => {
  ElMessage.success('通知设置保存成功')
}

const handleTest = () => {
  ElMessage.success('测试通知已发送')
}
</script>

<style scoped lang="scss">
.notification-settings {
  // 样式
}
</style>
