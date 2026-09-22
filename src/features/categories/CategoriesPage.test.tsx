import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'
import { categoryFixtures } from '../../test/handlers'
import { server } from '../../test/server'

describe('CategoriesPage', () => {
  beforeEach(() => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    window.history.pushState({}, '', '/categories')
  })

  it('lists categories and opens API-backed details', async () => {
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByText('Bebidas')).toBeVisible()
    expect(screen.getByText('2 categorias no catálogo')).toBeVisible()

    await user.click(screen.getByRole('button', { name: /ver bebidas/i }))

    const dialog = await screen.findByRole('dialog', {
      name: /detalhes da categoria/i,
    })
    expect(within(dialog).getByText('Bebidas e infusões')).toBeVisible()
  })

  it('normalizes an invalid page before requesting categories', async () => {
    window.history.replaceState({}, '', '/categories?page=-2')
    let requestedPage: string | null = null
    server.use(
      http.get('*/api/v1/categories', ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page')
        return HttpResponse.json({
          content: categoryFixtures,
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1,
        })
      }),
    )

    render(<App />)

    expect(await screen.findByText('Bebidas')).toBeVisible()
    expect(requestedPage).toBe('0')
  })

  it('stores sorting in the URL and resets pagination', async () => {
    window.history.replaceState({}, '', '/categories?page=2')
    const user = userEvent.setup()
    render(<App />)

    await screen.findByText('Bebidas')
    await user.selectOptions(
      screen.getByRole('combobox', { name: /ordenar categorias/i }),
      'name,desc',
    )

    const searchParams = new URLSearchParams(window.location.search)
    expect(searchParams.get('sort')).toBe('name')
    expect(searchParams.get('direction')).toBe('desc')
    expect(searchParams.has('page')).toBe(false)
  })

  it('renders an empty category catalog', async () => {
    server.use(
      http.get('*/api/v1/categories', () =>
        HttpResponse.json({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
    )
    render(<App />)

    expect(await screen.findByText(/nenhuma categoria cadastrada/i)).toBeVisible()
  })
})
