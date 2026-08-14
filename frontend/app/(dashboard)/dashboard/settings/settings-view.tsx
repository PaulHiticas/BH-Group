"use client"

import { useRouter } from "next/navigation"
import { ShieldAlert, ShieldCheck } from "lucide-react"
import { toast } from "sonner"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { MfaSetupFlow } from "@/components/auth/mfa-setup-flow"
import { useCurrentUser } from "@/hooks/use-current-user"
import { authApi } from "@/lib/api/auth"
import { useAuthStore } from "@/lib/stores/auth-store"

export function SettingsView() {
  const router = useRouter()
  const { data: user, isLoading } = useCurrentUser()
  const clearSession = useAuthStore((state) => state.clearSession)

  async function handleMfaSetupComplete() {
    await authApi.logout().catch(() => {})
    clearSession()
    toast.success("2FA activat! Autentifică-te din nou pentru a confirma.")
    router.push("/login")
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Setări cont</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Administrează securitatea contului tău de administrator.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            Autentificare în doi pași (2FA)
          </CardTitle>
          <CardDescription>
            Adaugă un cod suplimentar din aplicații precum Google Authenticator sau Authy la fiecare
            autentificare.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {isLoading || !user ? (
            <Skeleton className="h-10 w-full" />
          ) : user.mfaEnabled ? (
            <>
              <div className="flex items-center gap-2 rounded-lg bg-emerald-500/10 px-3 py-2 text-sm text-emerald-600 dark:text-emerald-400">
                <ShieldCheck className="size-4" />
                2FA este activ pe contul tău.
              </div>
              <p className="text-xs text-muted-foreground">
                2FA este obligatoriu pentru toate conturile și nu poate fi dezactivat din cont. Dacă
                ai pierdut accesul la aplicația de autentificare, poți reconfigura mai jos (necesită
                parola) sau cere unui Super Admin să-l reseteze.
              </p>

              <MfaSetupFlow
                onComplete={handleMfaSetupComplete}
                completeLabel="Continuă la autentificare"
                requirePasswordConfirmation
              />
            </>
          ) : (
            <>
              <div className="flex items-center gap-2 rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-600 dark:text-amber-400">
                <ShieldAlert className="size-4" />
                2FA nu este activat momentan.
              </div>

              <MfaSetupFlow onComplete={handleMfaSetupComplete} completeLabel="Continuă la autentificare" />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
