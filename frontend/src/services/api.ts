import type { AuthResponse, Dashboard } from '../types'

const API_URL = import.meta.env.VITE_API_URL || '/api/v1'

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('fintrack_token')
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${API_URL}${path}`, { ...options, headers })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message ?? 'Something went wrong')
  return body as T
}

export const api = {
  login: (email: string, password: string) => request<AuthResponse>('/auth/login', { method:'POST', body: JSON.stringify({email,password}) }),
  register: (name: string, email: string, password: string) => request<AuthResponse>('/auth/register', { method:'POST', body: JSON.stringify({name,email,password}) }),
  dashboard: (month?: string) => request<Dashboard>(
    `/analytics/dashboard${month ? `?month=${encodeURIComponent(month)}` : ''}`
  ),
  logout: () => request<void>('/auth/logout', { method:'POST' })
}
