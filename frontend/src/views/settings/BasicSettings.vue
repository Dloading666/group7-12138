<template>
  <div class="basic-settings">
    <el-card>
      <template #header>
        <span>基础设置</span>
      </template>

      <el-form :model="settingsForm" :rules="rules" ref="settingsFormRef" label-width="140px" style="max-width: 800px;">
        <el-divider content-position="left">系统配置</el-divider>
        
        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="settingsForm.systemName" placeholder="请输入系统名称" />
        </el-form-item>

        <el-form-item label="系统Logo" prop="logo">
          <el-upload
            class="logo-uploader"
            action="#"
            :show-file-list="false"
          >
            <img v-if="settingsForm.logo" :src="settingsForm.logo" class="logo" />
            <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="系统描述" prop="description">
          <el-input v-model="settingsForm.description" type="textarea" :rows="3" placeholder="请输入系统描述" />
        </el-form-item>

        <el-divider content-position="left">任务配置</el-divider>

        <el-form-item label="默认超时时间" prop="defaultTimeout">
          <el-input-number v-model="settingsForm.defaultTimeout" :min="30" :max="3600" />
          <span class="unit">秒</span>
        </el-form-item>

        <el-form-item label="最大并发任务数" prop="maxConcurrency">
          <el-input-number v-model="settingsForm.maxConcurrency" :min="1" :max="100" />
        </el-form-item>

        <el-form-item label="任务重试次数" prop="retryCount">
          <el-input-number v-model="settingsForm.retryCount" :min="0" :max="10" />
        </el-form-item>

        <el-form-item label="任务保留天数" prop="taskRetentionDays">
          <el-input-number v-model="settingsForm.taskRetentionDays" :min="1" :max="365" />
          <span class="unit">天</span>
        </el-form-item>

        <el-divider content-position="left">安全设置</el-divider>

        <el-form-item label="会话超时时间" prop="sessionTimeout">
          <el-input-number v-model="settingsForm.sessionTimeout" :min="5" :max="1440" />
          <span class="unit">分钟</span>
        </el-form-item>

        <el-form-item label="密码强度要求">
          <el-checkbox-group v-model="settingsForm.passwordPolicy">
            <el-checkbox value="uppercase">包含大写字母</el-checkbox>
            <el-checkbox value="lowercase">包含小写字母</el-checkbox>
            <el-checkbox value="number">包含数字</el-checkbox>
            <el-checkbox value="special">包含特殊字符</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="最小密码长度" prop="minPasswordLength">
          <el-input-number v-model="settingsForm.minPasswordLength" :min="6" :max="20" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave">保存设置</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const settingsFormRef = ref(null)

const settingsForm = reactive({
  systemName: '管理系统',
  logo: '',
  description: '自动化任务管理与执行平台',
  defaultTimeout: 300,
  maxConcurrency: 10,
  retryCount: 3,
  taskRetentionDays: 30,
  sessionTimeout: 30,
  passwordPolicy: ['uppercase', 'lowercase', 'number'],
  minPasswordLength: 8
})

const rules = {
  systemName: [{ required: true, message: '请输入系统名称', trigger: 'blur' }]
}

const handleSave = () => {
  settingsFormRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success('设置保存成功')
    }
  })
}

const handleReset = () => {
  settingsFormRef.value.resetFields()
}
</script>

<style scoped lang="scss">
.basic-settings {
  .logo-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;

      &:hover {
        border-color: #409eff;
      }
    }

    .logo {
      width: 178px;
      height: 178px;
      display: block;
    }

    .logo-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 178px;
      height: 178px;
      line-height: 178px;
      text-align: center;
    }
  }

  .unit {
    margin-left: 10px;
    color: #999;
  }
}
</style>
