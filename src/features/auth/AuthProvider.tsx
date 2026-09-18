import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { setUnauthorizedHandler } from '../../services/api-client'
import type { LoginRequest } from '../../services/contracts/auth'
import { getCurrentUser, login as requestLogin } from './auth-api'
import { AuthContext, type AuthContextValue } from './auth-context'
import { authStorage } from './auth-storage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [token, setToken] = useState(authStorage.getToken)

  const currentUserQuery = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
    enabled: Boolean(token),
    retry: false,
  })

  const logout = useCallback(() => {
    authStorage.clearToken()
    setToken(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(undefined)
  }, [logout])

  const login = useCallback(
    async (input: LoginRequest) => {
      const response = await requestLogin(input)
      authStorage.setToken(response.token)
      setToken(response.token)
      await queryClient.fetchQuery({
        queryKey: ['currentUser'],
        queryFn: getCurrentUser,
      })
    },
    [queryClient],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      user: currentUserQuery.data ?? null,
      isRestoring: Boolean(token) && currentUserQuery.isPending,
      login,
      logout,
    }),
    [currentUserQuery.data, currentUserQuery.isPending, login, logout, token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
