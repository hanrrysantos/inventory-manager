import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import {
  createCategory,
  deleteCategory,
  getCategories,
  getCategory,
  updateCategory,
} from './categories-api'

describe('categories API', () => {
  it('requests and returns a page of categories', async () => {
    let requestedUrl = ''
    server.use(
      http.get('*/api/v1/categories', ({ request }) => {
        requestedUrl = request.url
        return HttpResponse.json({
          content: [],
          page: 1,
          size: 20,
          totalElements: 25,
          totalPages: 2,
        })
      }),
    )

    const result = await getCategories({ page: 1, size: 20, sort: 'name,desc' })

    const searchParams = new URL(requestedUrl).searchParams
    expect(searchParams.get('page')).toBe('1')
    expect(searchParams.get('size')).toBe('20')
    expect(searchParams.get('sort')).toBe('name,desc')
    expect(result.totalElements).toBe(25)
  })

  it('requests one category by id', async () => {
    server.use(
      http.get('*/api/v1/categories/:id', ({ params }) =>
        HttpResponse.json({
          id: Number(params.id),
          name: 'Bebidas',
          description: 'Líquidos',
        }),
      ),
    )

    await expect(getCategory(4)).resolves.toMatchObject({ id: 4 })
  })

  it('sends the supported category mutation payloads', async () => {
    const requests: Array<[string, string, unknown]> = []
    server.use(
      http.post('*/api/v1/categories', async ({ request }) => {
        const body = await request.json()
        requests.push([request.method, new URL(request.url).pathname, body])
        return HttpResponse.json({ id: 4, ...(body as object) }, { status: 201 })
      }),
      http.put('*/api/v1/categories/:id', async ({ request }) => {
        const body = await request.json()
        requests.push([request.method, new URL(request.url).pathname, body])
        return HttpResponse.json({ id: 4, ...(body as object) })
      }),
      http.delete('*/api/v1/categories/:id', ({ request }) => {
        requests.push([request.method, new URL(request.url).pathname, null])
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await createCategory({ name: 'Bebidas', description: 'Líquidos' })
    await updateCategory(4, { name: 'Bebidas frias', description: '' })
    await deleteCategory(4)

    expect(requests).toEqual([
      ['POST', '/api/v1/categories', { name: 'Bebidas', description: 'Líquidos' }],
      ['PUT', '/api/v1/categories/4', { name: 'Bebidas frias', description: '' }],
      ['DELETE', '/api/v1/categories/4', null],
    ])
  })
})
