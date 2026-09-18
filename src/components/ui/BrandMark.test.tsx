import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BrandMark } from './BrandMark'

describe('BrandMark', () => {
  it('shows the EstoqueHub identity with a decorative logo', () => {
    const { container } = render(<BrandMark />)

    expect(screen.getByText('EstoqueHub')).toBeVisible()
    expect(screen.getByText('Seu estoque, sempre sob controle')).toBeVisible()
    expect(container.querySelector('img')).toHaveAttribute(
      'src',
      expect.stringContaining('estoquehub-logo'),
    )
  })

  it('supports the inverse treatment used on dark surfaces', () => {
    render(<BrandMark inverse />)

    expect(screen.getByTestId('brand-wordmark')).toHaveClass('text-white')
  })
})
