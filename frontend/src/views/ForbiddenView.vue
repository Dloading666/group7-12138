<template>
  <div class="status-page surface-panel" data-testid="forbidden-page">
    <div class="status-code">403</div>
    <h1>无权限访问</h1>
    <p>当前账号没有权限查看这个页面，或者管理员尚未为你勾选该功能范围。</p>
    <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
  </div>
</template>

<style scoped lang="scss">
.status-page {
  min-height: calc(100vh - 32px);
  margin: 16px;
  display: grid;
  place-items: center;
  text-align: center;
  background: linear-gradient(180deg, rgba(255,255,255,0.96), rgba(248,250,255,0.96));
}
.status-code {
  font-size: 96px;
  font-weight: 800;
  color: var(--app-primary);
  line-height: 1;
}
h1 { margin: 8px 0 0; }
p { max-width: 520px; color: var(--app-text-muted); margin: 8px 0 24px; }
</style>
