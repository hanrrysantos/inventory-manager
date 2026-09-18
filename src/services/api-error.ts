import axios from 'axios'

interface ApiErrorBody {
  message?: string
}

export function getApiErrorMessage(
  error: unknown,
  fallback = 'Não foi possível concluir a solicitação.',
): string {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return fallback
  return error.response?.data?.message || fallback
}
