import { useAuthStore } from "@/lib/stores/auth-store"
import {
  ApiError,
  type ApiErrorEnvelope,
  type ApiSuccessEnvelope,
  type AuthResponse,
} from "@/lib/api/types"

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1"

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = useAuthStore.getState().refreshToken
  if (!refreshToken) return null

  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (res) => {
        if (!res.ok) {
          useAuthStore.getState().clearSession()
          return null
        }
        const envelope = (await res.json()) as ApiSuccessEnvelope<AuthResponse>
        if (!envelope.data) {
          useAuthStore.getState().clearSession()
          return null
        }
        useAuthStore.getState().setSession(envelope.data)
        return envelope.data.accessToken
      })
      .catch(() => {
        useAuthStore.getState().clearSession()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

interface RequestOptions extends RequestInit {
  skipAuth?: boolean
  skipRefreshOn401?: boolean
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { skipAuth, skipRefreshOn401, headers, ...rest } = options

  const doFetch = async (): Promise<Response> => {
    const accessToken = useAuthStore.getState().accessToken
    const isFormData = rest.body instanceof FormData
    const finalHeaders: HeadersInit = {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...headers,
      ...(accessToken && !skipAuth
        ? { Authorization: `Bearer ${accessToken}` }
        : {}),
    }

    return fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: finalHeaders,
    })
  }

  let response = await doFetch()

  if (response.status === 401 && !skipAuth && !skipRefreshOn401) {
    const newAccessToken = await refreshAccessToken()
    if (newAccessToken) {
      response = await doFetch()
    }
  }

  const isJson = response.headers
    .get("content-type")
    ?.includes("application/json")
  const body = isJson ? await response.json() : null

  if (!response.ok) {
    throw new ApiError(response.status, body as ApiErrorEnvelope)
  }

  return (body as ApiSuccessEnvelope<T>)?.data as T
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) =>
    apiRequest<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, data?: unknown, options?: RequestOptions) =>
    apiRequest<T>(path, {
      ...options,
      method: "POST",
      body: data !== undefined ? JSON.stringify(data) : undefined,
    }),
  patch: <T>(path: string, data?: unknown, options?: RequestOptions) =>
    apiRequest<T>(path, {
      ...options,
      method: "PATCH",
      body: data !== undefined ? JSON.stringify(data) : undefined,
    }),
  put: <T>(path: string, data?: unknown, options?: RequestOptions) =>
    apiRequest<T>(path, {
      ...options,
      method: "PUT",
      body: data !== undefined ? JSON.stringify(data) : undefined,
    }),
  delete: <T>(path: string, options?: RequestOptions) =>
    apiRequest<T>(path, { ...options, method: "DELETE" }),
  upload: <T>(path: string, formData: FormData, options?: RequestOptions) =>
    apiRequest<T>(path, { ...options, method: "POST", body: formData }),
}
