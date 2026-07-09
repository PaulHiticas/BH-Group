"use client"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { toast } from "sonner"
import { authApi, type LoginPayload } from "@/lib/api/auth"
import { ApiError, isMfaChallenge } from "@/lib/api/types"
import { useAuthStore } from "@/lib/stores/auth-store"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useLogin() {
  const router = useRouter()
  const setSession = useAuthStore((state) => state.setSession)
  const setMfaChallengeToken = useAuthStore((state) => state.setMfaChallengeToken)

  return useMutation({
    mutationFn: (payload: LoginPayload) => authApi.login(payload),
    onSuccess: (result) => {
      if (isMfaChallenge(result)) {
        setMfaChallengeToken(result.challengeToken)
        router.push("/mfa/verify")
        return
      }
      setSession(result)
      toast.success(`Bine ai revenit, ${result.user.firstName}!`)
      router.push("/dashboard")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Autentificarea a eșuat. Verifică datele introduse."))
    },
  })
}

export function useVerifyMfaLogin() {
  const router = useRouter()
  const setSession = useAuthStore((state) => state.setSession)

  return useMutation({
    mutationFn: (code: string) => {
      const challengeToken = useAuthStore.getState().mfaChallengeToken
      if (!challengeToken) {
        throw new Error("Sesiunea de autentificare a expirat. Te rugăm să te autentifici din nou.")
      }
      return authApi.verifyMfaLogin({ challengeToken, code })
    },
    onSuccess: (result) => {
      setSession(result)
      toast.success(`Bine ai revenit, ${result.user.firstName}!`)
      router.push("/dashboard")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Cod de verificare invalid."))
    },
  })
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (email: string) => authApi.forgotPassword(email),
    onSuccess: () => {
      toast.success("Dacă adresa există în sistem, vei primi un email cu instrucțiuni.")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "A apărut o eroare. Încearcă din nou."))
    },
  })
}

export function useResetPassword() {
  const router = useRouter()

  return useMutation({
    mutationFn: ({ token, newPassword }: { token: string; newPassword: string }) =>
      authApi.resetPassword(token, newPassword),
    onSuccess: () => {
      toast.success("Parola a fost resetată cu succes. Te poți autentifica acum.")
      router.push("/login")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Link-ul de resetare este invalid sau a expirat."))
    },
  })
}

export function useSetupMfa() {
  return useMutation({
    mutationFn: () => authApi.setupMfa(),
    onError: (error) => {
      toast.error(errorMessage(error, "Nu am putut porni configurarea 2FA."))
    },
  })
}

export function useEnableMfa() {
  const router = useRouter()
  const clearSession = useAuthStore((state) => state.clearSession)

  return useMutation({
    mutationFn: (code: string) => authApi.enableMfa(code),
    onSuccess: async () => {
      const refreshToken = useAuthStore.getState().refreshToken
      if (refreshToken) {
        await authApi.logout(refreshToken).catch(() => {})
      }
      clearSession()
      toast.success("2FA activat! Autentifică-te din nou pentru a confirma.")
      router.push("/login")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Cod invalid. Încearcă din nou."))
    },
  })
}

export function useDisableMfa() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (password: string) => authApi.disableMfa(password),
    onSuccess: () => {
      toast.success("Autentificarea în doi pași a fost dezactivată.")
      queryClient.invalidateQueries({ queryKey: ["current-user"] })
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Parolă incorectă."))
    },
  })
}

export function useLogout() {
  const router = useRouter()
  const clearSession = useAuthStore((state) => state.clearSession)

  return useMutation({
    mutationFn: () => {
      const refreshToken = useAuthStore.getState().refreshToken
      if (!refreshToken) return Promise.resolve()
      return authApi.logout(refreshToken)
    },
    onSettled: () => {
      clearSession()
      router.push("/login")
    },
  })
}
