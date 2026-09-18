import { http, HttpResponse } from 'msw'

export const authenticatedUser = {
  id: 1,
  name: 'Ana Souza',
  email: 'ana@email.com',
  role: 'ADMIN' as const,
  createdAt: '2026-09-17T12:00:00',
}

export const productFixtures = [
  {
    id: 1,
    name: 'Chá Verde Orgânico',
    sku: 'CHA-001',
    totalQuantity: 142,
    categoryName: 'Bebidas',
    minStock: 40,
  },
  {
    id: 2,
    name: 'Mel Silvestre 500g',
    sku: 'MEL-014',
    totalQuantity: 28,
    categoryName: 'Alimentos',
    minStock: 30,
  },
  {
    id: 3,
    name: 'Sabonete Natural Lavanda',
    sku: 'SAB-207',
    totalQuantity: 0,
    categoryName: 'Cosméticos',
    minStock: 25,
  },
]

export const dashboardFixture = {
  totalQuantity: 170,
  productCount: 3,
  lowStockCount: 1,
  outOfStockCount: 1,
  expiredBatchCount: 2,
  inventoryValue: 21480,
  attentionItems: [
    {
      productId: 3,
      productName: 'Sabonete Natural Lavanda',
      sku: 'SAB-207',
      quantity: 0,
      minStock: 25,
      status: 'OUT_OF_STOCK' as const,
    },
    {
      productId: 2,
      productName: 'Mel Silvestre 500g',
      sku: 'MEL-014',
      quantity: 28,
      minStock: 30,
      status: 'LOW_STOCK' as const,
    },
  ],
}

export const handlers = [
  http.post('*/api/v1/auth/register', async ({ request }) => {
    const input = (await request.json()) as {
      name: string
      email: string
      password: string
    }
    return HttpResponse.json(
      {
        id: 2,
        name: input.name,
        email: input.email,
        role: 'USER',
        createdAt: '2026-09-18T12:00:00',
      },
      {
        status: 201,
        headers: { Location: '/api/v1/auth/register/2' },
      },
    )
  }),
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
  http.get('*/api/v1/products', () => HttpResponse.json(productFixtures)),
  http.get('*/api/v1/dashboard/summary', () =>
    HttpResponse.json(dashboardFixture),
  ),
]
