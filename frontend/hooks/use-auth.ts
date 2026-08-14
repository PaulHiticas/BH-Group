"use client"

import { useMutation, useQuery } from "@tanstack/react-query"
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

export function useVerifyMfaRecoveryLogin() {
  const router = useRouter()
  const setSession = useAuthStore((state) => state.setSession)

  return useMutation({
    mutationFn: (recoveryCode: string) => {
      const challengeToken = useAuthStore.getState().mfaChallengeToken
      if (!challengeToken) {
        throw new Error("Sesiunea de autentificare a expirat. Te rugăm să te autentifici din nou.")
      }
      return authApi.verifyMfaRecovery({ challengeToken, recoveryCode })
    },
    onSuccess: (result) => {
      setSession(result)
      toast.success(`Bine ai revenit, ${result.user.firstName}!`)
      router.push("/dashboard")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Cod de recuperare invalid sau deja folosit."))
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

export function useInviteInfo(token: string | null) {
  return useQuery({
    queryKey: ["invite-info", token],
    queryFn: () => authApi.getInvite(token as string),
    enabled: !!token,
    retry: false,
  })
}

export function useAcceptInvite() {
  const router = useRouter()
  const setSession = useAuthStore((state) => state.setSession)

  return useMutation({
    mutationFn: ({ token, password }: { token: string; password: string }) =>
      authApi.acceptInvite(token, password),
    onSuccess: (result) => {
      setSession(result)
      toast.success(`Bine ai venit, ${result.user.firstName}!`)
      router.push("/dashboard")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Invitația este invalidă sau a expirat."))
    },
  })
}

export function useSetupMfa() {
  return useMutation({
    mutationFn: (password?: string) => authApi.setupMfa(password),
    onError: (error) => {
      toast.error(errorMessage(error, "Nu am putut porni configurarea 2FA."))
    },
  })
}

/**
 * Deliberately no onSuccess side effects here (unlike other mutations in
 * this file): the response carries one-time recovery codes that must be
 * shown to the user before anything else happens (logout, redirect, etc.),
 * so the calling component decides what "done" means once it has displayed
 * and the user has acknowledged them - see MfaSetupFlow.
 */
export function useEnableMfa() {
  return useMutation({
    mutationFn: (code: string) => authApi.enableMfa(code),
    onError: (error) => {
      toast.error(errorMessage(error, "Cod invalid. Încearcă din nou."))
    },
  })
}


export function useLogout() {
  const router = useRouter()
  const clearSession = useAuthStore((state) => state.clearSession)

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => {
      clearSession()
      router.push("/login")
    },
  })
}
