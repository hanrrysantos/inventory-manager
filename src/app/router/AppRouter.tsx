import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '../../components/layout/AppLayout'
import { LoginPage } from '../../features/auth/LoginPage'
import { ProtectedRoute } from '../../features/auth/ProtectedRoute'
import { useAuth } from '../../features/auth/use-auth'

function DashboardPlaceholder() {
  return (
    <main className="p-5 md:p-8">
      <h2 className="text-lg font-semibold">Resumo do estoque</h2>
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
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPlaceholder />} />
          <Route path="/products" element={<ProductsPlaceholder />} />
        </Route>
      </Route>
      <Route
        path="*"
        element={<Navigate to={user ? '/dashboard' : '/login'} replace />}
      />
    </Routes>
  )
}
