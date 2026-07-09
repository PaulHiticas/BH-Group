"use client"

import { useState } from "react"
import { QRCodeSVG } from "qrcode.react"
import { ShieldAlert, ShieldCheck } from "lucide-react"
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
import { useCurrentUser } from "@/hooks/use-current-user"
import { useDisableMfa, useEnableMfa, useSetupMfa } from "@/hooks/use-auth"

export function SettingsView() {
  const { data: user, isLoading } = useCurrentUser()
  const setupMfa = useSetupMfa()
  const enableMfa = useEnableMfa()
  const disableMfa = useDisableMfa()

  const [enableCode, setEnableCode] = useState("")
  const [disablePassword, setDisablePassword] = useState("")
  const [showDisableForm, setShowDisableForm] = useState(false)

  function handleStartSetup() {
    setupMfa.mutate()
  }

  function handleConfirmEnable() {
    enableMfa.mutate(enableCode, {
      onSuccess: () => {
        setEnableCode("")
        setupMfa.reset()
      },
    })
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

              {!setupMfa.data ? (
                <Button onClick={handleStartSetup} disabled={setupMfa.isPending} className="w-fit">
                  {setupMfa.isPending ? "Se generează..." : "Activează 2FA"}
                </Button>
              ) : (
                <div className="flex flex-col items-center gap-4 rounded-lg border border-border/60 p-6">
                  <p className="text-sm text-muted-foreground">
                    Scanează codul cu Google Authenticator sau Authy, apoi introdu codul generat.
                  </p>
                  <div className="rounded-lg bg-white p-4">
                    <QRCodeSVG value={setupMfa.data.otpAuthUrl} size={180} />
                  </div>
                  <p className="break-all text-center text-xs text-muted-foreground">
                    Sau introdu manual cheia: <code className="font-mono">{setupMfa.data.secret}</code>
                  </p>
                  <div className="flex w-full max-w-52 flex-col gap-2">
                    <Label htmlFor="enable-code">Cod de 6 cifre</Label>
                    <Input
                      id="enable-code"
                      inputMode="numeric"
                      maxLength={6}
                      placeholder="123456"
                      value={enableCode}
                      onChange={(e) => setEnableCode(e.target.value.replace(/\D/g, ""))}
                      className="text-center text-lg tracking-widest"
                    />
                  </div>
                  <div className="flex gap-2">
                    <Button
                      disabled={enableCode.length !== 6 || enableMfa.isPending}
                      onClick={handleConfirmEnable}
                    >
                      {enableMfa.isPending ? "Se confirmă..." : "Confirmă și activează"}
                    </Button>
                    <Button variant="ghost" onClick={() => setupMfa.reset()}>
                      Anulează
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
