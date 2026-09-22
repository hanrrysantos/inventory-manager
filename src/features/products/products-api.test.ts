import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { getProducts } from './products-api'

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
})
