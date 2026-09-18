import { http, HttpResponse } from 'msw'

export const authenticatedUser = {
  id: 1,
  name: 'Ana Souza',
  email: 'ana@email.com',
  role: 'ADMIN' as const,
  createdAt: '2026-09-17T12:00:00',
}

export const handlers = [
  http.post('*/api/v1/auth/login', async ({ request }) => {
    const credentials = (await request.json()) as {
      email: string
      password: string
    }

    if (
      credentials.email !== 'admin@email.com' ||
      credentials.password !== 'admin123'
    ) {
      return HttpResponse.json(
        {
          instant: '2026-09-17T12:00:00Z',
          status: 401,
          error: 'Unauthorized',
          message: 'E-mail ou senha inválidos',
          path: '/api/v1/auth/login',
        },
        { status: 401 },
      )
    }

    return HttpResponse.json({ token: 'valid-token' })
  }),
  http.get('*/api/v1/users/me', ({ request }) => {
    if (request.headers.get('Authorization') !== 'Bearer valid-token') {
      return HttpResponse.json(
        {
          status: 401,
          error: 'Unauthorized',
          message: 'Token ausente ou inválido',
        },
        { status: 401 },
      )
    }

    return HttpResponse.json(authenticatedUser)
  }),
]
