import { apiClient } from '../../services/api-client'
import type { AuthResponse, LoginRequest } from '../../services/contracts/auth'
import type { User } from '../../services/contracts/user'

export async function login(input: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/api/v1/auth/login', input)
  return data
}

export async function getCurrentUser(): Promise<User> {
  const { data } = await apiClient.get<User>('/api/v1/users/me')
  return data
}
