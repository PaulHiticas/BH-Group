import { apiClient } from "@/lib/api/client"
import type {
  AuthResponse,
  InviteInfoResponse,
  LoginResult,
  UserResponse,
} from "@/lib/api/types"

export interface LoginPayload {
  email: string
  password: string
}

export interface MfaVerifyLoginPayload {
  challengeToken: string
  code: string
}

export interface MfaRecoveryLoginPayload {
  challengeToken: string
  recoveryCode: string
}

export interface MfaSetupResponse {
  secret: string
  otpAuthUrl: string
}

export interface MfaEnableResponse {
  /** Shown to the user exactly once, right after enabling - never retrievable again. */
  recoveryCodes: string[]
}

export const authApi = {
  login: (payload: LoginPayload) =>
    apiClient.post<LoginResult>("/auth/login", payload, { skipAuth: true }),

  verifyMfaLogin: (payload: MfaVerifyLoginPayload) =>
    apiClient.post<AuthResponse>("/auth/mfa/verify-login", payload, {
      skipAuth: true,
    }),

  verifyMfaRecovery: (payload: MfaRecoveryLoginPayload) =>
    apiClient.post<AuthResponse>("/auth/mfa/verify-recovery", payload, {
      skipAuth: true,
    }),

  refresh: () =>
    apiClient.post<AuthResponse>("/auth/refresh", undefined, { skipAuth: true }),

  logout: () => apiClient.post<void>("/auth/logout"),

  forgotPassword: (email: string) =>
    apiClient.post<void>("/auth/forgot-password", { email }, { skipAuth: true }),

  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<void>(
      "/auth/reset-password",
      { token, newPassword },
      { skipAuth: true }
    ),

  getInvite: (token: string) =>
    apiClient.get<InviteInfoResponse>(`/auth/invite/${token}`, { skipAuth: true }),

  acceptInvite: (token: string, password: string) =>
    apiClient.post<AuthResponse>(
      "/auth/invite/accept",
      { token, password },
      { skipAuth: true }
    ),

  // Password is required (and checked server-side) only when the account
  // already has MFA enabled - reconfiguring/re-enrolling needs proof beyond
  // just holding a valid access token. Omitted for first-time setup.
  setupMfa: (password?: string) =>
    apiClient.post<MfaSetupResponse>("/auth/mfa/setup", password ? { password } : undefined),

  enableMfa: (code: string) => apiClient.post<MfaEnableResponse>("/auth/mfa/enable", { code }),

  me: () => apiClient.get<UserResponse>("/auth/me"),
}
