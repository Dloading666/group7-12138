import type { AuthMenuNode } from '@/types/auth'

export function filterMenuTree(tree: AuthMenuNode[], permissionCodes: string[]) {
  const codeSet = new Set(permissionCodes)
  const walk = (nodes: AuthMenuNode[]): AuthMenuNode[] =>
    nodes
      .map((node) => {
        const children = node.children ? walk(node.children) : []
        const visible = !node.permissionCode || codeSet.has(node.permissionCode)
        if (!visible && children.length === 0) return null
        return {
          ...node,
          children
        }
      })
      .filter(Boolean) as AuthMenuNode[]

  return walk(tree)
}

export function findFirstLeafPath(tree: AuthMenuNode[]): string {
  for (const node of tree) {
    if (node.path) return node.path
    if (node.children?.length) {
      const child = findFirstLeafPath(node.children)
      if (child) return child
    }
  }
  return '/dashboard'
}

export function flattenMenuTree(tree: AuthMenuNode[]): AuthMenuNode[] {
  const result: AuthMenuNode[] = []
  const walk = (nodes: AuthMenuNode[]) => {
    nodes.forEach((node) => {
      result.push(node)
      if (node.children?.length) walk(node.children)
    })
  }
  walk(tree)
  return result
}

