import { expect, type Page } from '@playwright/test'
import { appRoutes } from './routes'
import { selectors } from './selectors'
import type { MockBackendHandle } from './mock-backend'

export async function loginAs(page: Page, backend: MockBackendHandle, username: 'admin' | 'operator' | 'viewer') {
  const credentials = {
    admin: { username: 'admin', password: 'admin123' },
    operator: { username: 'operator', password: 'user123' },
    viewer: { username: 'viewer', password: 'viewer123' }
  }[username]

  await page.goto(appRoutes.login)
  await page.evaluate(() => {
    window.localStorage.removeItem('rpa_token')
    window.localStorage.removeItem('rpa_user')
    window.localStorage.removeItem('rpa_permissions')
    window.localStorage.removeItem('rpa_menu_tree')
  })
  await page.goto(appRoutes.login)
  await expect(page.locator(selectors.loginPage)).toBeVisible()
  await page.locator(selectors.loginUsername).fill(credentials.username)
  await page.locator(selectors.loginPassword).fill(credentials.password)
  await page.locator(selectors.loginSubmit).click()
  await expect(page.locator(selectors.appShell)).toBeVisible()
  await page.waitForLoadState('networkidle')
}

export async function ensureDesktopSurface(page: Page) {
  await expect(page.locator('body')).toBeVisible()
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth
    }
  })
  expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 2)
}

export async function expectForbiddenView(page: Page) {
  await page.waitForLoadState('networkidle')
  const forbiddenVisible = await page.locator(selectors.forbiddenPage).isVisible().catch(() => false)
  if (forbiddenVisible) {
    await expect(page.locator(selectors.forbiddenPage)).toBeVisible()
    return
  }
  await expect(page).toHaveURL(/403/)
}
