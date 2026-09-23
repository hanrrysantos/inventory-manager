export interface Category {
  id: number
  name: string
  description: string | null
}

export interface CategoryInput {
  name: string
  description: string
}
