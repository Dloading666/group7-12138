export function hasPermission(permissionCodes: string[], code?: string) {
  if (!code) return true
  return permissionCodes.includes(code)
}

export function hasAnyPermission(permissionCodes: string[], codes?: string[]) {
  if (!codes || codes.length === 0) return true
  return codes.some((code) => permissionCodes.includes(code))
}

export function diffPermissions(baseIds: number[], nextIds: number[]) {
  const base = new Set(baseIds)
  const next = new Set(nextIds)
  const grants = Array.from(next).filter((id) => !base.has(id))
  const revokes = Array.from(base).filter((id) => !next.has(id))
  return { grants, revokes }
}

