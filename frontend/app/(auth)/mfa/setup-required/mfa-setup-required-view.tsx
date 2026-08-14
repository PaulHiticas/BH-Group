"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { ShieldAlert } from "lucide-react"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { MfaSetupFlow } from "@/components/auth/mfa-setup-flow"
import { useAuthStore } from "@/lib/stores/auth-store"

export function MfaSetupRequiredView() {
  const router = useRouter()
  // Checked once on mount, same reasoning as MfaVerifyForm: this page is only
  // ever reached via a redirect from an already-authenticated session that
  // got blocked by the backend's mandatory-MFA gate, not a fresh visit.
  const [hasSession] = useState(() => !!useAuthStore.getState().accessToken)

  useEffect(() => {
    if (!hasSession) {
      router.replace("/login")
    }
  }, [hasSession, router])

  function handleComplete() {
    router.push("/dashboard")
  }

  if (!hasSession) {
    return null
  }

  return (
    <Card className="border-white/15 bg-background/95 shadow-2xl backdrop-blur-md">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-xl">
          <ShieldAlert className="size-5 text-amber-500" />
          Configurare obligatorie 2FA
        </CardTitle>
        <CardDescription>
          Contul tău necesită autentificare în doi pași. Nu poți continua în aplicație până nu o
          configurezi.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <MfaSetupFlow onComplete={handleComplete} completeLabel="Continuă către aplicație" />
      </CardContent>
    </Card>
  )
}
