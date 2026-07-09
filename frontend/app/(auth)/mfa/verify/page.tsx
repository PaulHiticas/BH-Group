import type { Metadata } from "next"
import { MfaVerifyForm } from "./mfa-verify-form"

export const metadata: Metadata = {
  title: "Verificare în doi pași",
}

export default function MfaVerifyPage() {
  return <MfaVerifyForm />
}
