import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../../features/auth/LoginPage'
import { ProtectedRoute } from '../../features/auth/ProtectedRoute'
import { useAuth } from '../../features/auth/use-auth'

function DashboardPlaceholder() {
  const { user } = useAuth()
  return (
    <main className="p-8">
      <h1 className="text-2xl font-semibold">Painel de estoque</h1>
      <p>{user?.name}</p>
    </main>
  )
}

function ProductsPlaceholder() {
  return <h1>Produtos</h1>
}

export function AppRouter() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPlaceholder />} />
        <Route path="/products" element={<ProductsPlaceholder />} />
      </Route>
      <Route
        path="*"
        element={<Navigate to={user ? '/dashboard' : '/login'} replace />}
      />
    </Routes>
  )
}
