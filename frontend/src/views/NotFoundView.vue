<template>
  <div class="status-page surface-panel">
    <div class="status-code">404</div>
    <h1>页面不存在</h1>
    <p>你访问的页面不存在，或者还没有被加入到当前路由表。</p>
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

