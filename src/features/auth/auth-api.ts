import { apiClient } from '../../services/api-client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
} from '../../services/contracts/auth'
import type { User } from '../../services/contracts/user'

function isUser(data: unknown): data is User {
  if (!data || typeof data !== 'object') return false

  const user = data as Record<string, unknown>
  return (
    typeof user.id === 'number' &&
    typeof user.name === 'string' &&
    user.name.trim().length > 0 &&
    typeof user.email === 'string' &&
    (user.role === 'ADMIN' || user.role === 'USER') &&
    typeof user.createdAt === 'string'
  )
}

export async function login(input: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/api/v1/auth/login', input)
  return data
}

export async function registerAccount(
  input: RegisterRequest,
): Promise<RegisterResponse> {
  const { data } = await apiClient.post<RegisterResponse>(
    '/api/v1/auth/register',
    input,
  )
  return data
}

export async function getCurrentUser(): Promise<User> {
  const { data } = await apiClient.get<unknown>('/api/v1/users/me')
  if (!isUser(data)) throw new Error('Resposta de sessão inválida')
  return data
}
