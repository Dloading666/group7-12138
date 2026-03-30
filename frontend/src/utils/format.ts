export function formatDateTime(value?: string | number | Date | null) {
  if (!value) return '-'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date)
}

export function formatPercent(value: number) {
  return `${Math.round(value * 10) / 10}%`
}

export function formatNumber(value?: number | null) {
  return typeof value === 'number' ? new Intl.NumberFormat('zh-CN').format(value) : '-'
}

