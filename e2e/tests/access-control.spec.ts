import { test, expect } from '@playwright/test'
import { createMockBackend } from './support/mock-backend'
import { loginAs, expectForbiddenView } from './support/app'
import { appRoutes } from './support/routes'
import { selectors } from './support/selectors'

test.describe('access control', () => {
  test('ordinary users cannot access role management', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    await loginAs(page, backend, 'operator')
    await page.goto(appRoutes.roleManagement)
    await expectForbiddenView(page)
  })

  test('ordinary users cannot access permission management', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    await loginAs(page, backend, 'viewer')
    await page.goto(appRoutes.permissionManagement)
    await expectForbiddenView(page)
  })

  test('admin can access system pages and sees the user/role/permission sections', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    await loginAs(page, backend, 'admin')
    await page.goto(appRoutes.roleManagement)
    await expect(page.locator(selectors.roleManagementPage)).toBeVisible()

    await page.goto(appRoutes.userManagement)
    await expect(page.locator(selectors.userManagementPage)).toBeVisible()

    await page.goto(appRoutes.permissionManagement)
    await expect(page.locator(selectors.permissionManagementPage)).toBeVisible()
  })
})
