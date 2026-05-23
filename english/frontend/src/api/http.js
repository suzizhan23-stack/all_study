const TOKEN_KEY = 'auth_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const BASE = '/api'

async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  }

  const res = await fetch(`${BASE}${url}`, { ...options, headers })

  if (res.status === 401) {
    removeToken()
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }

  const body = await res.json()
  if (!res.ok) {
    throw new Error(body.message || `HTTP ${res.status}`)
  }
  return body.data
}

export const api = {
  get(url, params) {
    const qs = params ? '?' + new URLSearchParams(
      Object.entries(params).filter(([_, v]) => v !== undefined && v !== null)
    ).toString() : ''
    return request(url + qs)
  },
  post(url, data) {
    return request(url, { method: 'POST', body: JSON.stringify(data) })
  },
  put(url, data) {
    return request(url, { method: 'PUT', body: JSON.stringify(data) })
  },
  delete(url, data) {
    return request(url, { method: 'DELETE', body: data ? JSON.stringify(data) : undefined })
  },
}
