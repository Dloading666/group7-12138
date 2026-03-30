import { test, expect } from '@playwright/test'
import { createMockBackend } from './support/mock-backend'
import { loginAs } from './support/app'
import { selectors } from './support/selectors'
import { appRoutes } from './support/routes'

test.describe('permission scope flows', () => {
  test('administrator grants task menu and create button, then user can see them', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    await loginAs(page, backend, 'admin')
    await expect(page.locator(selectors.dashboardPage)).toBeVisible()
    await expect(page.locator(selectors.appSidebar)).toBeVisible()

    backend.setUserScope(2, 'taskMenuAndCreate')

    await page.goto(appRoutes.login)
    await loginAs(page, backend, 'operator')

    await page.goto(appRoutes.taskList)
    await expect(page.locator(selectors.taskListPage)).toBeVisible()
    await expect(page.locator(selectors.taskCreateButton)).toBeVisible()
  })

  test('revoking task create hides only the button while keeping the task page accessible', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    backend.setUserScope(2, 'taskMenuOnly')

    await loginAs(page, backend, 'operator')
    await page.goto(appRoutes.taskList)

    await expect(page.locator(selectors.taskListPage)).toBeVisible()
    await expect(page.locator(selectors.taskFilterBar)).toBeVisible()
    await expect(page.locator(selectors.taskTable)).toBeVisible()
    await expect(page.locator(selectors.taskCreateButton)).toHaveCount(0)
  })

  test('revoking the whole task menu hides the entry and blocks direct access', async ({ page }) => {
    const backend = createMockBackend()
    await backend.install(page)

    backend.setUserScope(2, 'noTaskMenu')

    await loginAs(page, backend, 'operator')
    await expect(page.locator(selectors.taskListPage)).toHaveCount(0)

    await page.goto(appRoutes.taskList)
    await expect(page).toHaveURL(/403|login/)
  })
})
