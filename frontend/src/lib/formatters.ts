const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const roleLabels: Record<string, string> = {
  ADMIN: 'Administradora',
  USER: 'Usuária',
}

export function formatCurrency(value: number): string {
  return currencyFormatter.format(value)
}

export function formatRole(role: string): string {
  return roleLabels[role] ?? role
}
