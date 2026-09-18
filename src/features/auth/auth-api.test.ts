import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { registerAccount } from './auth-api'

describe('registerAccount', () => {
  it('posts the backend contract and returns the created user', async () => {
    let submittedBody: unknown
    server.use(
      http.post('*/api/v1/auth/register', async ({ request }) => {
        submittedBody = await request.json()
        return HttpResponse.json(
          {
            id: 2,
            name: 'Maria Silva',
            email: 'maria@example.com',
            role: 'USER',
            createdAt: '2026-09-18T12:00:00',
          },
          { status: 201 },
        )
      }),
    )

    const result = await registerAccount({
      name: 'Maria Silva',
      email: 'maria@example.com',
      password: '123456',
    })

    expect(submittedBody).toEqual({
      name: 'Maria Silva',
      email: 'maria@example.com',
      password: '123456',
    })
    expect(result.email).toBe('maria@example.com')
  })
})
