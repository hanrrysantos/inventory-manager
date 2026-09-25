export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface RegisterResponse {
  id: number
  name: string
  email: string
  role: 'ADMIN' | 'USER'
  createdAt: string
}
