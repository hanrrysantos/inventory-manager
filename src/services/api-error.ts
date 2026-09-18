import axios from 'axios'

interface ApiErrorBody {
  message?: string
}

export function getApiErrorMessage(
  error: unknown,
  fallback = 'Não foi possível concluir a solicitação.',
): string {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return fallback

  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return 'O servidor demorou para responder. Aguarde alguns segundos e tente novamente.'
  }

  if (!error.response) {
    return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'
  }

  return error.response?.data?.message || fallback
}
