import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'
import { authenticatedUser, categoryFixtures } from '../../test/handlers'
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

  it('allows an admin to create a category', async () => {
    let requestBody: unknown
    server.use(
      http.post('*/api/v1/categories', async ({ request }) => {
        requestBody = await request.json()
        return HttpResponse.json({ id: 3, ...(requestBody as object) }, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /nova categoria/i }))
    await user.type(screen.getByLabelText('Nome'), 'Higiene')
    await user.type(screen.getByLabelText(/descrição/i), 'Cuidados pessoais')
    await user.click(screen.getByRole('button', { name: /salvar categoria/i }))

    expect(requestBody).toEqual({
      name: 'Higiene',
      description: 'Cuidados pessoais',
    })
    expect(await screen.findByText('Categoria criada.')).toBeVisible()
  })

  it('allows an admin to edit a category', async () => {
    let requestBody: unknown
    server.use(
      http.put('*/api/v1/categories/:id', async ({ request }) => {
        requestBody = await request.json()
        return HttpResponse.json({ id: 1, ...(requestBody as object) })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /editar bebidas/i }))
    const name = screen.getByLabelText('Nome')
    await user.clear(name)
    await user.type(name, 'Bebidas frias')
    await user.click(screen.getByRole('button', { name: /salvar alterações/i }))

    expect(requestBody).toEqual({
      name: 'Bebidas frias',
      description: 'Bebidas e infusões',
    })
    expect(await screen.findByText('Categoria atualizada.')).toBeVisible()
  })

  it('allows an admin to delete a category after confirmation', async () => {
    let requestedId = ''
    server.use(
      http.delete('*/api/v1/categories/:id', ({ params }) => {
        requestedId = String(params.id)
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /excluir bebidas/i }))
    await user.click(screen.getByRole('button', { name: /confirmar exclusão/i }))

    expect(requestedId).toBe('1')
    expect(await screen.findByText('Categoria excluída.')).toBeVisible()
  })

  it('keeps category administration hidden from regular users', async () => {
    server.use(
      http.get('*/api/v1/users/me', () =>
        HttpResponse.json({ ...authenticatedUser, role: 'USER' }),
      ),
    )
    render(<App />)

    expect(await screen.findByText('Bebidas')).toBeVisible()
    expect(screen.queryByRole('button', { name: /nova categoria/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /editar bebidas/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /excluir bebidas/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /ver bebidas/i })).toBeVisible()
  })

  it('keeps a failed delete confirmation open with the API message', async () => {
    server.use(
      http.delete('*/api/v1/categories/:id', () =>
        HttpResponse.json(
          { message: 'Esta categoria possui produtos' },
          { status: 409 },
        ),
      ),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /excluir bebidas/i }))
    await user.click(screen.getByRole('button', { name: /confirmar exclusão/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Esta categoria possui produtos',
    )
    expect(
      screen.getByRole('dialog', { name: /excluir categoria/i }),
    ).toBeVisible()
  })
})
