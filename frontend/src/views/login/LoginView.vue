<template>
  <div class="login-page" data-testid="login-page">
    <div class="login-shell">
      <section class="login-brand">
        <div class="brand-content">
          <h1>RPA 管理系统</h1>
        </div>
      </section>

      <section class="login-form-panel surface-panel">
        <div class="login-title">
          <div class="eyebrow">欢迎回来</div>
          <h2>登录系统</h2>
          <p>请输入管理员或被授权账号继续，密码下方需要完成验证码验证。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          data-testid="login-form"
          @keyup.enter="submit"
        >
          <el-form-item prop="username" data-testid="login-username">
            <el-input
              ref="usernameInputRef"
              v-model="form.username"
              size="large"
              placeholder="请输入用户名"
              :prefix-icon="User"
              autocomplete="username"
              clearable
              name="username"
            />
          </el-form-item>

          <el-form-item prop="password" data-testid="login-password">
            <el-input
              ref="passwordInputRef"
              v-model="form.password"
              size="large"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
              autocomplete="current-password"
              name="password"
            />
          </el-form-item>

          <el-form-item prop="captcha" class="captcha-field" data-testid="login-captcha">
            <div class="captcha-row">
              <el-input
                ref="captchaInputRef"
                v-model="form.captcha"
                size="large"
                placeholder="请输入验证码"
                :maxlength="CAPTCHA_LENGTH"
                autocomplete="off"
                clearable
                name="captcha"
              />
              <button
                type="button"
                class="captcha-trigger"
                data-testid="login-captcha-refresh"
                aria-label="刷新验证码"
                title="点击刷新验证码"
                @click="refreshCaptcha()"
              >
                <img
                  :key="captchaMotionKey"
                  class="captcha-image"
                  :src="captchaImage"
                  alt="图形验证码"
                />
                <span>点击刷新</span>
              </button>
            </div>
          </el-form-item>

          <p class="form-hint">请输入图片中的字母和数字，验证码不区分大小写，点击右侧图片即可重新生成。</p>

          <el-button
            type="primary"
            size="large"
            class="login-btn"
            data-testid="login-submit"
            :loading="loading"
            @click="submit"
          >
            登录进入
          </el-button>
        </el-form>

        <div class="demo-box">
          <div class="demo-title">测试账号</div>
          <div class="demo-list">
            <button type="button" class="demo-item" @click="fillDemo('admin', 'admin123')">
              admin / admin123
            </button>
            <button type="button" class="demo-item" @click="fillDemo('user01', 'user123')">
              user01 / user123
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type InputInstance } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { findFirstLeafPath } from '@/utils/menu'

type LoginFormModel = {
  username: string
  password: string
  captcha: string
}

const CAPTCHA_LENGTH = 4
const CAPTCHA_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const usernameInputRef = ref<InputInstance>()
const passwordInputRef = ref<InputInstance>()
const captchaInputRef = ref<InputInstance>()
const loading = ref(false)
const captchaCode = ref('')
const captchaImage = ref('')
const captchaMotionKey = ref(0)

const form = reactive<LoginFormModel>({
  username: '',
  password: '',
  captcha: ''
})

function generateCaptchaCode(length = CAPTCHA_LENGTH) {
  return Array.from({ length }, () => CAPTCHA_CHARS[Math.floor(Math.random() * CAPTCHA_CHARS.length)]).join('')
}

function randomBetween(min: number, max: number) {
  return min + Math.random() * (max - min)
}

function createCaptchaImage(code: string) {
  const width = 164
  const height = 56
  const chars = code
    .split('')
    .map((char, index) => {
      const x = 26 + index * 26 + randomBetween(-3, 3)
      const y = 36 + randomBetween(-4, 5)
      const rotate = randomBetween(-18, 18)
      const size = randomBetween(24, 30)
      const color = ['#11325c', '#2058a6', '#2d6de2', '#4b7cff'][Math.floor(Math.random() * 4)]
      return `<text x="${x}" y="${y}" font-size="${size}" transform="rotate(${rotate} ${x} ${y})" fill="${color}" font-family="Consolas, Monaco, monospace" font-weight="700">${char}</text>`
    })
    .join('')

  const lines = Array.from({ length: 3 }, () => {
    const x1 = randomBetween(6, width * 0.35)
    const y1 = randomBetween(8, height - 8)
    const x2 = randomBetween(width * 0.55, width - 6)
    const y2 = randomBetween(8, height - 8)
    const opacity = randomBetween(0.2, 0.45).toFixed(2)
    return `<path d="M${x1} ${y1} C ${randomBetween(36, 72)} ${randomBetween(0, height)}, ${randomBetween(92, 126)} ${randomBetween(0, height)}, ${x2} ${y2}" stroke="rgba(59,130,246,${opacity})" stroke-width="${randomBetween(1.4, 2.2).toFixed(1)}" fill="none" stroke-linecap="round" />`
  }).join('')

  const dots = Array.from({ length: 18 }, () => {
    const radius = randomBetween(0.8, 2.3).toFixed(1)
    const opacity = randomBetween(0.18, 0.35).toFixed(2)
    return `<circle cx="${randomBetween(8, width - 8)}" cy="${randomBetween(8, height - 8)}" r="${radius}" fill="rgba(17,50,92,${opacity})" />`
  }).join('')

  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
      <defs>
        <linearGradient id="captcha-bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#f8fbff" />
          <stop offset="100%" stop-color="#dbeafe" />
        </linearGradient>
      </defs>
      <rect width="${width}" height="${height}" rx="16" fill="url(#captcha-bg)" />
      <rect x="1" y="1" width="${width - 2}" height="${height - 2}" rx="15" fill="none" stroke="rgba(96, 165, 250, 0.35)" />
      ${lines}
      ${dots}
      ${chars}
    </svg>
  `

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function refreshCaptcha(clearInput = true) {
  captchaCode.value = generateCaptchaCode()
  captchaImage.value = createCaptchaImage(captchaCode.value)
  captchaMotionKey.value += 1

  if (clearInput) {
    form.captcha = ''
  }

  formRef.value?.clearValidate('captcha')
}

function readInputValue(inputRef: InputInstance | undefined, fallback: string) {
  return inputRef?.input?.value ?? fallback
}

function syncFormFromInputs() {
  form.username = readInputValue(usernameInputRef.value, form.username).trim()
  form.password = readInputValue(passwordInputRef.value, form.password)
  form.captcha = readInputValue(captchaInputRef.value, form.captcha).trim()
}

const rules: FormRules<LoginFormModel> = {
  username: [
    {
      validator: (_rule, value: string, callback) => {
        if (!String(value || '').trim()) {
          callback(new Error('请输入用户名'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (!String(value || '')) {
          callback(new Error('请输入密码'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  captcha: [
    {
      validator: (_rule, value: string, callback) => {
        const input = String(value || '').trim().toUpperCase()
        if (!input) {
          callback(new Error('请输入验证码'))
          return
        }
        if (input !== captchaCode.value.toUpperCase()) {
          callback(new Error('验证码不正确'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ]
}

watch(
  () => form.username,
  (value) => {
    if (String(value || '').trim()) {
      formRef.value?.clearValidate('username')
    }
  }
)

watch(
  () => form.password,
  (value) => {
    if (String(value || '')) {
      formRef.value?.clearValidate('password')
    }
  }
)

watch(
  () => form.captcha,
  (value) => {
    if (String(value || '').trim()) {
      formRef.value?.clearValidate('captcha')
    }
  }
)

function fillDemo(username: string, password: string) {
  form.username = username
  form.password = password
  formRef.value?.clearValidate(['username', 'password'])
}

async function submit() {
  if (!formRef.value) return

  syncFormFromInputs()

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true

  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password
    })
    ElMessage.success('登录成功')
    refreshCaptcha()
    await router.replace(findFirstLeafPath(authStore.menuTree))
  } catch (error) {
    refreshCaptcha()
    const handledError = error as Error & { __toastHandled?: boolean }
    if (!handledError.__toastHandled) {
      ElMessage.error(handledError.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}

refreshCaptcha()
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100svh;
  padding: 20px;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 12% 18%, rgba(59, 130, 246, 0.24), transparent 30%),
    radial-gradient(circle at 84% 14%, rgba(56, 189, 248, 0.2), transparent 24%),
    radial-gradient(circle at 76% 76%, rgba(37, 99, 235, 0.1), transparent 24%),
    linear-gradient(135deg, #06111d 0%, #0c1d31 46%, #edf4ff 46%, #f8fbff 100%);
}

.login-shell {
  width: min(1320px, 100%);
  min-height: min(860px, calc(100svh - 40px));
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(420px, 0.85fr);
  overflow: hidden;
  border-radius: 36px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 36px 96px rgba(3, 12, 27, 0.28);
  animation: shell-rise 0.7s cubic-bezier(0.2, 0.7, 0.2, 1);
}

.login-brand {
  position: relative;
  overflow: hidden;
  padding: clamp(42px, 5vw, 78px);
  color: #eff6ff;
  background:
    linear-gradient(180deg, rgba(8, 22, 39, 0.2), rgba(8, 22, 39, 0.55)),
    linear-gradient(135deg, #091521 0%, #10283f 48%, #14375f 100%);

  &::before,
  &::after {
    content: '';
    position: absolute;
    inset: auto;
    border-radius: 999px;
    filter: blur(14px);
    opacity: 0.72;
  }

  &::before {
    width: 340px;
    height: 340px;
    top: -120px;
    right: -80px;
    background: radial-gradient(circle, rgba(96, 165, 250, 0.36), rgba(96, 165, 250, 0) 70%);
  }

  &::after {
    width: 280px;
    height: 280px;
    left: -120px;
    bottom: -120px;
    background: radial-gradient(circle, rgba(14, 165, 233, 0.28), rgba(14, 165, 233, 0) 70%);
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  min-height: 100%;
  width: 100%;
  max-width: 640px;
  text-align: center;
  animation: content-rise 0.72s 0.08s both;
}

.brand-content h1 {
  margin: 0;
  max-width: none;
  font-size: clamp(60px, 8vw, 120px);
  line-height: 0.92;
  letter-spacing: -0.08em;
}

.login-form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 28px;
  padding: clamp(38px, 4.5vw, 74px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(244, 248, 255, 0.98)),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent 32%);
  border: 1px solid rgba(255, 255, 255, 0.48);
  animation: content-rise 0.72s 0.16s both;
}

.login-title {
  display: grid;
  gap: 12px;
}

.login-title .eyebrow {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--app-primary);
  text-transform: uppercase;
}

.login-title h2 {
  margin: 0;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1;
  letter-spacing: -0.06em;
  color: #0f172a;
}

.login-title p {
  margin: 0;
  max-width: 32ch;
  font-size: 16px;
  line-height: 1.48;
  color: var(--app-text-muted);
}

.login-form {
  display: grid;
  gap: 12px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-input__wrapper) {
    min-height: 58px;
    padding-inline: 16px;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.92);
    box-shadow:
      0 0 0 1px rgba(148, 163, 184, 0.2),
      0 16px 28px rgba(15, 23, 42, 0.04);
    transition:
      transform 0.2s ease,
      box-shadow 0.2s ease,
      background-color 0.2s ease;
  }

  :deep(.el-input__wrapper:hover) {
    transform: translateY(-1px);
    box-shadow:
      0 0 0 1px rgba(96, 165, 250, 0.22),
      0 18px 30px rgba(30, 64, 175, 0.08);
  }

  :deep(.el-input__wrapper.is-focus) {
    background: #fff;
    box-shadow:
      0 0 0 1px rgba(59, 130, 246, 0.45),
      0 20px 34px rgba(37, 99, 235, 0.12);
  }

  :deep(.el-input__inner) {
    font-size: 15px;
    line-height: 1.35;
    color: #0f172a;
  }

  :deep(.el-form-item__error) {
    padding-top: 8px;
    line-height: 1.35;
  }
}

.captcha-field {
  :deep(.el-form-item__content) {
    width: 100%;
  }
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 164px;
  gap: 12px;
  width: 100%;
}

.captcha-trigger {
  display: grid;
  place-items: center;
  gap: 3px;
  width: 100%;
  min-height: 58px;
  padding: 6px;
  border: 0;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(219, 234, 254, 0.76), rgba(191, 219, 254, 0.52));
  box-shadow:
    inset 0 0 0 1px rgba(96, 165, 250, 0.28),
    0 16px 28px rgba(37, 99, 235, 0.08);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;

  span {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #1d4ed8;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow:
      inset 0 0 0 1px rgba(59, 130, 246, 0.34),
      0 20px 34px rgba(37, 99, 235, 0.14);
  }
}

.captcha-image {
  width: 100%;
  height: 40px;
  object-fit: cover;
  border-radius: 12px;
  animation: captcha-pulse 0.24s ease;
}

.form-hint {
  margin: -2px 0 2px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-muted);
}

.login-btn {
  width: 100%;
  height: 54px;
  margin-top: 4px;
  border: 0;
  border-radius: 18px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.24);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;

  &:not(.is-loading):hover {
    transform: translateY(-1px);
    box-shadow: 0 22px 36px rgba(37, 99, 235, 0.28);
  }
}

.demo-box {
  display: grid;
  gap: 12px;
  padding-top: 4px;
}

.demo-title {
  font-size: 13px;
  color: var(--app-text-muted);
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.demo-item {
  padding: 10px 16px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--app-text);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(59, 130, 246, 0.38);
    color: var(--app-primary-strong);
  }
}

@keyframes shell-rise {
  from {
    opacity: 0;
    transform: translateY(22px) scale(0.985);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes content-rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes captcha-pulse {
  from {
    opacity: 0;
    transform: scale(0.96);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 1140px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-brand {
    min-height: 340px;
  }
}

@media (max-width: 720px) {
  .login-page {
    padding: 0;
  }

  .login-shell {
    min-height: 100svh;
    border-radius: 0;
  }

  .login-brand,
  .login-form-panel {
    padding: 28px 22px;
  }

  .brand-content h1 {
    font-size: clamp(46px, 17vw, 68px);
  }

  .captcha-row {
    grid-template-columns: minmax(0, 1fr) 136px;
  }
}

@media (max-width: 520px) {
  .captcha-row {
    grid-template-columns: 1fr;
  }

  .captcha-trigger {
    min-height: 72px;
  }

  .demo-list {
    flex-direction: column;
  }
}
</style>
