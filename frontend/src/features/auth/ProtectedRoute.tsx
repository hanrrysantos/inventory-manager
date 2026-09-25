import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './use-auth'

export function ProtectedRoute() {
  const { user, isRestoring } = useAuth()

  if (isRestoring) {
    return (
      <main className="grid min-h-screen place-items-center" role="status">
        Carregando sessão...
      </main>
    )
  }

  if (!user) return <Navigate to="/login" replace />

  return <Outlet />
}
