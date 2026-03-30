import { test, expect } from '@playwright/test'
import { createMockBackend } from './support/mock-backend'
import { loginAs, ensureDesktopSurface } from './support/app'
import { selectors } from './support/selectors'
import { appRoutes } from './support/routes'

test.describe('layout stability', () => {
  test('login page matches the shared visual system and stays within the viewport', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    await page.goto(appRoutes.login)
    await expect(page.locator(selectors.loginPage)).toBeVisible()
    await expect(page.locator(selectors.loginForm)).toBeVisible()
    await expect(page.locator(selectors.loginSubmit)).toBeVisible()
    await ensureDesktopSurface(page)
  })

  test('dashboard layout renders the shell, hero and KPI blocks without overflow', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    await loginAs(page, backend, 'admin')
    await page.goto(appRoutes.dashboard)

    await expect(page.locator(selectors.dashboardPage)).toBeVisible()
    await expect(page.locator(selectors.dashboardHero)).toBeVisible()
    await expect(page.locator(selectors.dashboardKpiTotal)).toBeVisible()
    await expect(page.locator(selectors.dashboardKpiRunning)).toBeVisible()
    await expect(page.locator(selectors.dashboardKpiRobot)).toBeVisible()
    await expect(page.locator(selectors.dashboardKpiSuccessRate)).toBeVisible()
    await expect(page.locator(selectors.dashboardTrendChart)).toBeVisible()
    await expect(page.locator(selectors.dashboardStatusChart)).toBeVisible()
    await expect(page.locator(selectors.dashboardRecentTasks)).toBeVisible()
    await ensureDesktopSurface(page)
  })

  test('task list layout renders filters, table and pagination cleanly', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    await loginAs(page, backend, 'operator')
    await page.goto(appRoutes.taskList)

    await expect(page.locator(selectors.taskListPage)).toBeVisible()
    await expect(page.locator(selectors.taskFilterBar)).toBeVisible()
    await expect(page.locator(selectors.taskTable)).toBeVisible()
    await expect(page.locator(selectors.taskPagination)).toBeVisible()
    await ensureDesktopSurface(page)
  })
})
