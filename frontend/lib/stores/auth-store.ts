import { create } from "zustand"
import { persist } from "zustand/middleware"
import type { AuthResponse, UserResponse } from "@/lib/api/types"

const SESSION_COOKIE_NAME = "pms_session"

function setSessionCookie() {
  if (typeof document === "undefined") return
  document.cookie = `${SESSION_COOKIE_NAME}=1; path=/; max-age=${60 * 60 * 24 * 30}; SameSite=Lax`
}

function clearSessionCookie() {
  if (typeof document === "undefined") return
  document.cookie = `${SESSION_COOKIE_NAME}=; path=/; max-age=0; SameSite=Lax`
}

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserResponse | null
  mfaChallengeToken: string | null
  setSession: (auth: AuthResponse) => void
  setAccessToken: (accessToken: string) => void
  setMfaChallengeToken: (token: string | null) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      mfaChallengeToken: null,
      setSession: (auth) => {
        setSessionCookie()
        set({
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
          user: auth.user,
          mfaChallengeToken: null,
        })
      },
      setAccessToken: (accessToken) => set({ accessToken }),
      setMfaChallengeToken: (mfaChallengeToken) => set({ mfaChallengeToken }),
      clearSession: () => {
        clearSessionCookie()
        set({ accessToken: null, refreshToken: null, user: null, mfaChallengeToken: null })
      },
    }),
    {
      name: "pms-auth",
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
)
