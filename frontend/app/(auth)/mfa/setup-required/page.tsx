import type { Metadata } from "next"
import { MfaSetupRequiredView } from "./mfa-setup-required-view"

export const metadata: Metadata = {
  title: "Configurare obligatorie 2FA",
}

export default function MfaSetupRequiredPage() {
  return <MfaSetupRequiredView />
}
