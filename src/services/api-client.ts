import axios from 'axios'
import { authStorage } from '../features/auth/auth-storage'

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | undefined

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 90_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = authStorage.getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      unauthorizedHandler?.()
    }
    return Promise.reject(error)
  },
)

export function setUnauthorizedHandler(
  handler: UnauthorizedHandler | undefined,
): void {
  unauthorizedHandler = handler
}
