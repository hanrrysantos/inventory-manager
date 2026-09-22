import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

HTMLDialogElement.prototype.showModal ??= function () {
  this.open = true
}

HTMLDialogElement.prototype.close ??= function () {
  this.open = false
  this.dispatchEvent(new Event('close'))
}

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
  localStorage.clear()
})

afterAll(() => server.close())
