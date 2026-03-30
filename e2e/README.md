# RPA E2E

This folder holds the Playwright scaffold for the new `D:\aaa\RPA` project.

## What this covers

- Login page and shared shell layout
- Dashboard and task list desktop layout checks
- Permission propagation flows for task menu and task create button
- Access control checks for role and permission management pages
- A mock backend layer that can be used before the real frontend/backend are ready

## Install

```powershell
cd D:\aaa\RPA\e2e
npm install
npm run install:browsers
```

## Run against a real app

Set the frontend URL and, if desired, a web server command:

```powershell
$env:E2E_BASE_URL = 'http://127.0.0.1:5173'
$env:E2E_WEB_SERVER_COMMAND = 'npm run dev -- --host 127.0.0.1 --port 5173'
$env:E2E_WEB_SERVER_CWD = 'D:\aaa\RPA\frontend'
npm test
```

If your backend is already running separately, point the frontend at it using the app's own env/config.

## Selector contract

The tests are written to prefer stable `data-testid` hooks. The frontend should add these hooks:

- `login-page`
- `login-form`
- `login-username`
- `login-password`
- `login-submit`
- `app-shell`
- `app-sidebar`
- `app-topbar`
- `app-breadcrumb`
- `dashboard-page`
- `dashboard-hero`
- `dashboard-kpi-total`
- `dashboard-kpi-running`
- `dashboard-kpi-robot`
- `dashboard-kpi-success-rate`
- `dashboard-trend-chart`
- `dashboard-status-chart`
- `dashboard-recent-tasks`
- `task-list-page`
- `task-filter-bar`
- `task-table`
- `task-pagination`
- `task-create-button`
- `user-management-page`
- `role-management-page`
- `permission-management-page`
- `forbidden-page`

For permission tree interactions, the frontend should also expose stable test ids for the permission tree root, node labels, and save button. The exact structure can be adapted, but the ids should stay consistent.

## Mock mode

`tests/support/mock-backend.ts` provides a stateful mock API for early validation. It supports:

- `/auth/login`
- `/auth/me`
- `/auth/logout`
- `/dashboard/overview`
- `/tasks`
- `/roles`
- `/permissions/tree`
- `/users/{id}/permissions/effective`
- `/users/{id}/permissions/overrides`

This lets the test suite be fleshed out before the real services are fully wired.

## Notes

- The current tests are designed for the final permission model: one fixed admin, one primary role per user, and user-level permission overrides.
- The mock backend is intentionally easy to replace with the real API calls later.
