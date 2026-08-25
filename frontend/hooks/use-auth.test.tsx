import type { ReactNode } from "react"
import { act, renderHook, waitFor } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { mockRouter } from "@/test/mocks/next-navigation"
import { useLogin } from "@/hooks/use-auth"
import { authApi } from "@/lib/api/auth"
import { useAuthStore } from "@/lib/stores/auth-store"

vi.mock("@/lib/api/auth", () => ({
  authApi: {
    login: vi.fn(),
  },
}))

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

const baseUser = {
  id: "user-1",
  email: "new@bhgroup.io",
  firstName: "New",
  lastName: "User",
  phone: null,
  role: "ADMINISTRATOR" as const,
  status: "ACTIVE" as const,
  emailVerified: true,
  createdAt: "2026-01-01T00:00:00Z",
}

describe("useLogin", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().clearSession()
  })

  it("routes an account without MFA straight to /mfa/setup-required, not /dashboard - so it never hits the mandatory-MFA gate and loops", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "access-token",
      tokenType: "Bearer",
      expiresIn: 3600,
      user: { ...baseUser, mfaEnabled: false },
    })

    const { result } = renderHook(() => useLogin(), { wrapper })
    act(() => {
      result.current.mutate({ email: "new@bhgroup.io", password: "x" })
    })

    await waitFor(() => expect(mockRouter.push).toHaveBeenCalledWith("/mfa/setup-required"))
    expect(mockRouter.push).not.toHaveBeenCalledWith("/dashboard")
    // The access token is still set - the setup page can render its QR flow
    // immediately, with no hard reload and no dependency on a blocked
    // endpoint.
    expect(useAuthStore.getState().accessToken).toBe("access-token")
  })

  it("routes an account with MFA already enabled straight to /dashboard, unchanged", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "access-token",
      tokenType: "Bearer",
      expiresIn: 3600,
      user: { ...baseUser, mfaEnabled: true },
    })

    const { result } = renderHook(() => useLogin(), { wrapper })
    act(() => {
      result.current.mutate({ email: "existing@bhgroup.io", password: "x" })
    })

    await waitFor(() => expect(mockRouter.push).toHaveBeenCalledWith("/dashboard"))
    expect(mockRouter.push).not.toHaveBeenCalledWith("/mfa/setup-required")
  })

  it("routes an MFA challenge to /mfa/verify without ever setting a session", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      challengeToken: "challenge-token",
      expiresIn: 300,
    })

    const { result } = renderHook(() => useLogin(), { wrapper })
    act(() => {
      result.current.mutate({ email: "mfa-user@bhgroup.io", password: "x" })
    })

    await waitFor(() => expect(mockRouter.push).toHaveBeenCalledWith("/mfa/verify"))
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(useAuthStore.getState().mfaChallengeToken).toBe("challenge-token")
  })
})
