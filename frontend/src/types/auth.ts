export interface UserProfile {
  id: number
  username: string
  realName?: string
  avatar?: string
  phone?: string
  email?: string
  status?: string
  role?: string
  roleName?: string
}

export interface AuthMenuNode {
  id?: number
  name: string
  path?: string
  icon?: string
  permissionCode?: string
  type?: 'MENU' | 'BUTTON' | 'API'
  hidden?: boolean
  children?: AuthMenuNode[]
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType?: string
  expiresIn?: number
  user: UserProfile
  permissionCodes: string[]
  menuTree: AuthMenuNode[]
}

export interface CurrentUserResult {
  user: UserProfile
  permissionCodes: string[]
  menuTree: AuthMenuNode[]
  role?: string
}

export interface PermissionNode {
  id: number
  name: string
  code: string
  type: 'MENU' | 'BUTTON' | 'API'
  parentId?: number | null
  path?: string
  icon?: string
  status?: string
  sortOrder?: number
  description?: string
  children?: PermissionNode[]
}

