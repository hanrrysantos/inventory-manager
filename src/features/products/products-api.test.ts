import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import {
  createProduct,
  deleteProduct,
  getLowStockProducts,
  getProduct,
  getProducts,
  updateProduct,
} from './products-api'

describe('getProducts', () => {
  it('requests and returns a page of products', async () => {
    let requestedUrl = ''
    server.use(
      http.get('*/api/v1/products', ({ request }) => {
        requestedUrl = request.url
        return HttpResponse.json({
          content: [],
          page: 1,
          size: 10,
          totalElements: 25,
          totalPages: 3,
        })
      }),
    )

    const result = await getProducts({ page: 1, size: 10, sort: 'name,desc' })

    const searchParams = new URL(requestedUrl).searchParams
    expect(searchParams.get('page')).toBe('1')
    expect(searchParams.get('size')).toBe('10')
    expect(searchParams.get('sort')).toBe('name,desc')
    expect(result).toEqual({
      content: [],
      page: 1,
      size: 10,
      totalElements: 25,
      totalPages: 3,
    })
  })

  it('requests one product by id', async () => {
    server.use(
      http.get('*/api/v1/products/:id', ({ params }) =>
        HttpResponse.json({
          id: Number(params.id),
          name: 'Café',
          sku: 'CAF-1',
          totalQuantity: 8,
          categoryName: 'Bebidas',
          minStock: 3,
        }),
      ),
    )

    await expect(getProduct(7)).resolves.toMatchObject({ id: 7 })
  })

  it('requests the paginated low-stock endpoint', async () => {
    let requestedUrl = ''
    server.use(
      http.get('*/api/v1/products/low-stock', ({ request }) => {
        requestedUrl = request.url
        return HttpResponse.json({
          content: [],
          page: 1,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        })
      }),
    )

    await getLowStockProducts({ page: 1, size: 20, sort: 'sku,desc' })

    const url = new URL(requestedUrl)
    expect(url.pathname).toBe('/api/v1/products/low-stock')
    expect(url.searchParams.get('sort')).toBe('sku,desc')
  })

  it('sends the supported product mutation payloads', async () => {
    const requests: Array<[string, string, unknown]> = []
    server.use(
      http.post('*/api/v1/products', async ({ request }) => {
        const body = await request.json()
        requests.push([request.method, new URL(request.url).pathname, body])
        return HttpResponse.json({
          id: 7,
          ...(body as object),
          totalQuantity: 0,
          categoryName: 'Bebidas',
        }, { status: 201 })
      }),
      http.put('*/api/v1/products/:id', async ({ request }) => {
        const body = await request.json()
        requests.push([request.method, new URL(request.url).pathname, body])
        return HttpResponse.json({
          id: 7,
          sku: 'CAF-1',
          totalQuantity: 0,
          categoryName: 'Bebidas',
          ...(body as object),
        })
      }),
      http.delete('*/api/v1/products/:id', ({ request }) => {
        requests.push([request.method, new URL(request.url).pathname, null])
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await createProduct({ name: 'Café', sku: 'CAF-1', minStock: 3, categoryId: 2 })
    await updateProduct(7, { name: 'Café especial', minStock: 5 })
    await deleteProduct(7)

    expect(requests).toEqual([
      ['POST', '/api/v1/products', { name: 'Café', sku: 'CAF-1', minStock: 3, categoryId: 2 }],
      ['PUT', '/api/v1/products/7', { name: 'Café especial', minStock: 5 }],
      ['DELETE', '/api/v1/products/7', null],
    ])
  })
})
