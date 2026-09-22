export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PageQuery {
  page: number
  size: number
  sort: `${string},${'asc' | 'desc'}`
}
