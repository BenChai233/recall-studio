type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

async function safeJson<T>(response: Response): Promise<T | null> {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text) as T
  } catch {
    return null
  }
}

type ApiResponse<T> = {
  code: string
  message: string
  data: T
}

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export async function request<T>(
  path: string,
  options: {
    method?: HttpMethod
    query?: Record<string, string | number | boolean | null | undefined>
    body?: unknown
  } = {},
): Promise<T> {
  const { method = 'GET', query, body } = options
  const queryString = query
    ? '?' +
      Object.entries(query)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(
          ([key, value]) =>
            `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`,
        )
        .join('&')
    : ''

  const response = await fetch(`${API_BASE}${path}${queryString}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
    },
    body: body ? JSON.stringify(body) : undefined,
  })

  const data = await safeJson<ApiResponse<T> & { message?: string; data?: T }>(response)
  if (!response.ok) {
    const message = data?.message || response.statusText || '请求失败'
    throw new Error(message)
  }
  if (data && typeof data === 'object' && 'data' in data) {
    return (data.data ?? (null as T)) as T
  }
  return (data ?? (null as T)) as T
}
