import { describe, expect, it } from 'vitest'
import { registerSchema } from './register-schema'

describe('registerSchema', () => {
  it('accepts the backend registration contract', () => {
    expect(
      registerSchema.safeParse({
        name: 'Maria Silva',
        email: 'maria@example.com',
        password: '123456',
      }).success,
    ).toBe(true)
  })

  it('rejects blank name, invalid email, and short password', () => {
    const result = registerSchema.safeParse({
      name: ' ',
      email: 'invalid',
      password: '123',
    })

    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues.map((issue) => issue.path[0])).toEqual([
        'name',
        'email',
        'password',
      ])
    }
  })
})
