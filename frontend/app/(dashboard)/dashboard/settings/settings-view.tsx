"use client"

import { useState } from "react"
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
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { MfaSetupFlow } from "@/components/auth/mfa-setup-flow"
import { useCurrentUser } from "@/hooks/use-current-user"
import { useDisableMfa } from "@/hooks/use-auth"
import { authApi } from "@/lib/api/auth"
import { useAuthStore } from "@/lib/stores/auth-store"

export function SettingsView() {
  const router = useRouter()
  const { data: user, isLoading } = useCurrentUser()
  const disableMfa = useDisableMfa()
  const clearSession = useAuthStore((state) => state.clearSession)

  const [disablePassword, setDisablePassword] = useState("")
  const [showDisableForm, setShowDisableForm] = useState(false)

  async function handleMfaSetupComplete() {
    await authApi.logout().catch(() => {})
    clearSession()
    toast.success("2FA activat! Autentifică-te din nou pentru a confirma.")
    router.push("/login")
  }

  function handleDisable() {
    disableMfa.mutate(disablePassword, {
      onSuccess: () => {
        setDisablePassword("")
        setShowDisableForm(false)
      },
    })
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

              {!showDisableForm ? (
                <Button variant="outline" onClick={() => setShowDisableForm(true)} className="w-fit">
                  Dezactivează 2FA
                </Button>
              ) : (
                <div className="flex flex-col gap-3 rounded-lg border border-border/60 p-4">
                  <Label htmlFor="disable-password">Confirmă parola pentru a dezactiva 2FA</Label>
                  <Input
                    id="disable-password"
                    type="password"
                    value={disablePassword}
                    onChange={(e) => setDisablePassword(e.target.value)}
                    autoComplete="current-password"
                  />
                  <div className="flex gap-2">
                    <Button
                      variant="destructive"
                      size="sm"
                      disabled={!disablePassword || disableMfa.isPending}
                      onClick={handleDisable}
                    >
                      {disableMfa.isPending ? "Se dezactivează..." : "Confirmă dezactivarea"}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setShowDisableForm(false)
                        setDisablePassword("")
                      }}
                    >
                      Anulează
                    </Button>
                  </div>
                </div>
              )}
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
