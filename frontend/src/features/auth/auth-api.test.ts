import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { googleLogin, linkGoogleAccount, registerAccount } from './auth-api'

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

describe('Google authentication API', () => {
  it('sends the Google ID token to the public login endpoint and returns the application token', async () => {
    let submittedBody: unknown
    server.use(
      http.post('*/api/v1/auth/google', async ({ request }) => {
        submittedBody = await request.json()
        return HttpResponse.json({ token: 'application-jwt' })
      }),
    )

    const result = await googleLogin('google-id-token')

    expect(submittedBody).toEqual({ idToken: 'google-id-token' })
    expect(result).toEqual({ token: 'application-jwt' })
  })

  it('sends the Google ID token to the authenticated account-linking endpoint', async () => {
    let submittedBody: unknown
    let authorization: string | null = null
    server.use(
      http.post('*/api/v1/auth/google/link', async ({ request }) => {
        submittedBody = await request.json()
        authorization = request.headers.get('Authorization')
        return new HttpResponse(null, { status: 204 })
      }),
    )
    localStorage.setItem('inventory-manager.token', 'application-jwt')

    await linkGoogleAccount('google-id-token')

    expect(submittedBody).toEqual({ idToken: 'google-id-token' })
    expect(authorization).toBe('Bearer application-jwt')
  })
})
