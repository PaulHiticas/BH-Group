import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen, renderWithProviders, waitFor } from "@/test/utils"
import { mockRouter } from "@/test/mocks/next-navigation"
import { MfaSetupRequiredView } from "./mfa-setup-required-view"
import { authApi } from "@/lib/api/auth"
import { useAuthStore } from "@/lib/stores/auth-store"

vi.mock("@/lib/api/auth", () => ({
  authApi: {
    refresh: vi.fn(),
    setupMfa: vi.fn(),
    enableMfa: vi.fn(),
  },
}))

describe("MfaSetupRequiredView", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().clearSession()
  })

  it("recovers the session via refresh and shows the setup flow when there's no in-memory access token (e.g. arriving via a hard-reload redirect), instead of bouncing to /login and looping", async () => {
    vi.mocked(authApi.refresh).mockResolvedValue({
      accessToken: "recovered-token",
      tokenType: "Bearer",
      expiresIn: 3600,
      user: {
        id: "user-1",
        email: "new@bhgroup.io",
        firstName: "New",
        lastName: "User",
        phone: null,
        role: "ADMINISTRATOR",
        status: "ACTIVE",
        emailVerified: true,
        mfaEnabled: false,
        createdAt: "2026-01-01T00:00:00Z",
      },
    })

    renderWithProviders(<MfaSetupRequiredView />)

    expect(await screen.findByRole("button", { name: "Configurează 2FA" })).toBeInTheDocument()
    expect(mockRouter.replace).not.toHaveBeenCalledWith("/login")
    expect(useAuthStore.getState().accessToken).toBe("recovered-token")
  })

  it("redirects to /login only when refresh itself fails - a genuinely logged-out visit, not a false negative", async () => {
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("no refresh cookie"))

    renderWithProviders(<MfaSetupRequiredView />)

    await waitFor(() => expect(mockRouter.replace).toHaveBeenCalledWith("/login"))
  })
})
