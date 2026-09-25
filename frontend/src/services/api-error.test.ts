import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it } from 'vitest'
import { getApiErrorMessage } from './api-error'

describe('getApiErrorMessage', () => {
  it.each(['ECONNABORTED', 'ETIMEDOUT']) (
    'explains that the server took too long for %s',
    (code) => {
      const error = new AxiosError('timeout', code)

      expect(getApiErrorMessage(error)).toBe(
        'O servidor demorou para responder. Aguarde alguns segundos e tente novamente.',
      )
    },
  )

  it('explains when the API cannot be reached', () => {
    const error = new AxiosError('Network Error', 'ERR_NETWORK')

    expect(getApiErrorMessage(error)).toBe(
      'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.',
    )
  })

  it('preserves the message returned by the API', () => {
    const error = new AxiosError(
      'Unauthorized',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      {
        data: { message: 'Invalid email or password' },
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: { headers: new AxiosHeaders() },
      },
    )

    expect(getApiErrorMessage(error)).toBe('Invalid email or password')
  })
})
