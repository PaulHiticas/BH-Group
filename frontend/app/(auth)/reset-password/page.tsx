import type { Metadata } from "next"
import { ResetPasswordView } from "./reset-password-view"

export const metadata: Metadata = {
  title: "Resetare parolă",
}

export default function ResetPasswordPage() {
  return <ResetPasswordView />
}
