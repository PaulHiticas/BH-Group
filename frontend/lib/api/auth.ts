import { apiClient } from "@/lib/api/client"
import type {
  AuthResponse,
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

export interface MfaSetupResponse {
  secret: string
  otpAuthUrl: string
}

export const authApi = {
  login: (payload: LoginPayload) =>
    apiClient.post<LoginResult>("/auth/login", payload, { skipAuth: true }),

  verifyMfaLogin: (payload: MfaVerifyLoginPayload) =>
    apiClient.post<AuthResponse>("/auth/mfa/verify-login", payload, {
      skipAuth: true,
    }),

  refresh: (refreshToken: string) =>
    apiClient.post<AuthResponse>(
      "/auth/refresh",
      { refreshToken },
      { skipAuth: true }
    ),

  logout: (refreshToken: string) =>
    apiClient.post<void>("/auth/logout", { refreshToken }),

  forgotPassword: (email: string) =>
    apiClient.post<void>("/auth/forgot-password", { email }, { skipAuth: true }),

  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<void>(
      "/auth/reset-password",
      { token, newPassword },
      { skipAuth: true }
    ),

  setupMfa: () => apiClient.post<MfaSetupResponse>("/auth/mfa/setup"),

  enableMfa: (code: string) => apiClient.post<void>("/auth/mfa/enable", { code }),

  disableMfa: (password: string) =>
    apiClient.post<void>("/auth/mfa/disable", { password }),

  me: () => apiClient.get<UserResponse>("/auth/me"),
}
