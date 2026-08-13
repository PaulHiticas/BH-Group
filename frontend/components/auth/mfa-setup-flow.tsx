"use client"

import { useState } from "react"
import { QRCodeSVG } from "qrcode.react"
import { Check, Copy, ShieldCheck } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useEnableMfa, useSetupMfa } from "@/hooks/use-auth"

interface MfaSetupFlowProps {
  /** Called after the user has acknowledged their recovery codes. */
  onComplete: () => void
  completeLabel: string
}

export function MfaSetupFlow({ onComplete, completeLabel }: MfaSetupFlowProps) {
  const setupMfa = useSetupMfa()
  const enableMfa = useEnableMfa()
  const [enableCode, setEnableCode] = useState("")
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null)
  const [copied, setCopied] = useState(false)
  const [acknowledged, setAcknowledged] = useState(false)

  function handleConfirmEnable() {
    enableMfa.mutate(enableCode, {
      onSuccess: (response) => {
        setRecoveryCodes(response.recoveryCodes)
        setEnableCode("")
      },
    })
  }

  function handleCopyCodes() {
    if (!recoveryCodes) return
    navigator.clipboard.writeText(recoveryCodes.join("\n")).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  if (recoveryCodes) {
    return (
      <div className="flex flex-col gap-4 rounded-lg border border-border/60 p-6">
        <div className="flex items-center gap-2 text-sm font-medium text-emerald-600 dark:text-emerald-400">
          <ShieldCheck className="size-4" />
          2FA activat cu succes
        </div>
        <p className="text-sm text-muted-foreground">
          Salvează aceste coduri de recuperare într-un loc sigur. Fiecare poate fi folosit o
          singură dată pentru a te autentifica dacă pierzi accesul la aplicația de autentificare.
          Nu vor mai fi afișate din nou.
        </p>
        <div className="grid grid-cols-2 gap-2 rounded-md bg-muted/50 p-4 font-mono text-sm">
          {recoveryCodes.map((code) => (
            <span key={code}>{code}</span>
          ))}
        </div>
        <Button type="button" variant="outline" size="sm" className="w-fit gap-2" onClick={handleCopyCodes}>
          {copied ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
          {copied ? "Copiat" : "Copiază codurile"}
        </Button>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={acknowledged}
            onChange={(e) => setAcknowledged(e.target.checked)}
            className="size-4"
          />
          Am salvat aceste coduri într-un loc sigur
        </label>
        <Button type="button" disabled={!acknowledged} onClick={onComplete}>
          {completeLabel}
        </Button>
      </div>
    )
  }

  if (!setupMfa.data) {
    return (
      <Button onClick={() => setupMfa.mutate()} disabled={setupMfa.isPending} className="w-fit">
        {setupMfa.isPending ? "Se generează..." : "Configurează 2FA"}
      </Button>
    )
  }

  return (
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
      <Button
        disabled={enableCode.length !== 6 || enableMfa.isPending}
        onClick={handleConfirmEnable}
      >
        {enableMfa.isPending ? "Se confirmă..." : "Confirmă și activează"}
      </Button>
    </div>
  )
}
