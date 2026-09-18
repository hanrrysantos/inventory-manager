import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { PageLoader } from '../../components/feedback/PageLoader'
import { AppLayout } from '../../components/layout/AppLayout'
import { ProtectedRoute } from '../../features/auth/ProtectedRoute'
import { useAuth } from '../../features/auth/use-auth'

const LoginPage = lazy(() =>
  import('../../features/auth/LoginPage').then(({ LoginPage }) => ({
    default: LoginPage,
  })),
)

const DashboardPage = lazy(() =>
  import('../../features/dashboard/DashboardPage').then(
    ({ DashboardPage }) => ({ default: DashboardPage }),
  ),
)

const ProductsPage = lazy(() =>
  import('../../features/products/ProductsPage').then(({ ProductsPage }) => ({
    default: ProductsPage,
  })),
)

export function AppRouter() {
  const { user } = useAuth()

  return (
    <Suspense fallback={<PageLoader label="Carregando página..." />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/products" element={<ProductsPage />} />
          </Route>
        </Route>
        <Route
          path="*"
          element={<Navigate to={user ? '/dashboard' : '/login'} replace />}
        />
      </Routes>
    </Suspense>
  )
}
