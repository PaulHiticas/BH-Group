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
  user: UserResponse | null
  mfaChallengeToken: string | null
  setSession: (auth: AuthResponse) => void
  setAccessToken: (accessToken: string) => void
  setMfaChallengeToken: (token: string | null) => void
  clearSession: () => void
}

// The refresh token lives only in the httpOnly `refresh_token` cookie the
// backend sets (see AuthCookieService) - it is never present in this store
// or in localStorage, so client-side JS (and any XSS) can't read it.
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      mfaChallengeToken: null,
      setSession: (auth) => {
        setSessionCookie()
        set({
          accessToken: auth.accessToken,
          user: auth.user,
          mfaChallengeToken: null,
        })
      },
      setAccessToken: (accessToken) => set({ accessToken }),
      setMfaChallengeToken: (mfaChallengeToken) => set({ mfaChallengeToken }),
      clearSession: () => {
        clearSessionCookie()
        set({ accessToken: null, user: null, mfaChallengeToken: null })
      },
    }),
    {
      name: "pms-auth",
      partialize: (state) => ({
        user: state.user,
      }),
    }
  )
)
