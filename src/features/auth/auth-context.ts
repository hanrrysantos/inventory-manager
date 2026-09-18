import { createContext } from 'react'
import type { LoginRequest } from '../../services/contracts/auth'
import type { User } from '../../services/contracts/user'

export interface AuthContextValue {
  user: User | null
  isRestoring: boolean
  login: (input: LoginRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
