"use client"

import { useState } from "react"
import { Download, Search, ShieldAlert } from "lucide-react"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useEraseGdprData, useExportGdprData, useGdprSearch } from "@/hooks/use-gdpr"
import type { GdprRecordType, GdprVerificationMethod } from "@/lib/api/types"

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", { dateStyle: "medium", timeStyle: "short" })
}

const RECORD_TYPE_LABELS: Record<GdprRecordType, string> = {
  RESERVATION: "Rezervare",
  LEAD: "Lead",
}

const VERIFICATION_METHOD_LABELS: Record<GdprVerificationMethod, string> = {
  EMAIL_CONFIRMATION: "Confirmare prin email",
  RESERVATION_DETAILS: "Detalii de rezervare verificate telefonic",
  IDENTITY_DOCUMENT: "Act de identitate verificat",
  OTHER: "Altă metodă",
}

const VERIFICATION_METHODS = Object.keys(VERIFICATION_METHOD_LABELS) as GdprVerificationMethod[]

/** Triggers a client-side download of already-fetched JSON - no email in the filename, nothing written server-side. */
function downloadJson(data: unknown, filenamePrefix: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" })
  const url = URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = `${filenamePrefix}-${Date.now()}.json`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function GdprView() {
  const [email, setEmail] = useState("")
  const [searchedEmail, setSearchedEmail] = useState("")

  const search = useGdprSearch()
  const exportData = useExportGdprData()
  const eraseData = useEraseGdprData()

  const [verificationMethod, setVerificationMethod] = useState<GdprVerificationMethod>("EMAIL_CONFIRMATION")
  const [verificationNote, setVerificationNote] = useState("")
  const [confirmEmailInput, setConfirmEmailInput] = useState("")

  const results = search.data
  const verificationNoteValid = verificationNote.trim().length > 0 && verificationNote.length <= 300
  const canActOnResults = !!results && results.length > 0 && verificationNoteValid
  const canConfirmErase = canActOnResults && confirmEmailInput.trim().toLowerCase() === searchedEmail.trim().toLowerCase()

  function handleSearch() {
    const trimmed = email.trim()
    if (!trimmed) return
    setSearchedEmail(trimmed)
    setConfirmEmailInput("")
    search.mutate(trimmed)
  }

  function handleExport() {
    if (!verificationNoteValid) return
    exportData.mutate(
      { email: searchedEmail, verification: { verificationMethod, verificationNote } },
      { onSuccess: (data) => downloadJson(data, "gdpr-export") }
    )
  }

  function handleErase() {
    if (!canConfirmErase) return
    eraseData.mutate(
      {
        email: searchedEmail,
        confirmationEmail: confirmEmailInput.trim(),
        verification: { verificationMethod, verificationNote },
      },
      {
        onSuccess: () => {
          setConfirmEmailInput("")
          search.mutate(searchedEmail) // refresh - erased records no longer match this email
        },
      }
    )
  }

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">GDPR — solicitări privind datele</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Caută toate rezervările și lead-urile asociate unei adrese de email, exportă-le sau
          anonimizează-le la cererea persoanei vizate. Rezervările nu sunt șterse (se păstrează
          datele/sumele în scop contabil), doar datele de identificare sunt eliminate — inclusiv
          linkul de auto-gestionare al rezervării, mesajele și notificările asociate.
        </p>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div className="relative min-w-72 flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="email@exemplu.com"
            className="pl-9"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          />
        </div>
        <Button type="button" disabled={!email.trim() || search.isPending} onClick={handleSearch}>
          Caută
        </Button>
      </div>

      {search.isPending ? (
        <Skeleton className="h-48 w-full" />
      ) : !search.isSuccess ? null : !results || results.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">
            Nicio înregistrare găsită pentru <span className="font-medium">{searchedEmail}</span>.
          </p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <p className="text-sm text-muted-foreground">
            {results.length} înregistrare(ări) găsite pentru <span className="font-medium">{searchedEmail}</span>
          </p>

          <div className="rounded-lg border border-border/60">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tip</TableHead>
                  <TableHead>Nume</TableHead>
                  <TableHead>Telefon</TableHead>
                  <TableHead>Context</TableHead>
                  <TableHead>Creat la</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {results.map((match) => (
                  <TableRow key={`${match.recordType}-${match.id}`}>
                    <TableCell>
                      <Badge variant="outline">{RECORD_TYPE_LABELS[match.recordType]}</Badge>
                    </TableCell>
                    <TableCell className="font-medium">{match.name}</TableCell>
                    <TableCell className="text-muted-foreground">{match.phone ?? "—"}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">{match.context}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatDateTime(match.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="flex flex-col gap-3 rounded-lg border p-4">
            <p className="text-sm font-medium">Verificarea identității solicitantului</p>
            <p className="text-xs text-muted-foreground">
              Obligatoriu înainte de export sau anonimizare. Nu introdu CNP, serie de act de
              identitate sau copii de acte în notă — doar cum a fost verificată identitatea.
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <Select value={verificationMethod} onValueChange={(v) => setVerificationMethod(v as GdprVerificationMethod)}>
                <SelectTrigger>
                  <SelectValue>{() => VERIFICATION_METHOD_LABELS[verificationMethod]}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {VERIFICATION_METHODS.map((m) => (
                    <SelectItem key={m} value={m}>
                      {VERIFICATION_METHOD_LABELS[m]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Textarea
                placeholder="Ex: am confirmat prin telefon numele și datele rezervării"
                value={verificationNote}
                onChange={(e) => setVerificationNote(e.target.value)}
                maxLength={300}
                className="sm:col-span-1"
              />
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="gap-2"
                disabled={!canActOnResults || exportData.isPending}
                onClick={handleExport}
              >
                <Download className="size-4" />
                Exportă (JSON)
              </Button>

              <AlertDialog>
                <AlertDialogTrigger
                  render={<Button variant="destructive" size="sm" className="gap-2" disabled={!canActOnResults} />}
                >
                  <ShieldAlert className="size-4" />
                  Anonimizează tot
                </AlertDialogTrigger>
                <AlertDialogContent className="sm:max-w-lg">
                  <AlertDialogHeader>
                    <AlertDialogTitle>Anonimizezi toate datele pentru {searchedEmail}?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Numele, emailul, telefonul, notele, codul de acces și linkul de
                      auto-gestionare vor fi șterse ireversibil, iar mesajele și notificările
                      legate de rezervările de mai jos vor fi redactate. Datele de rezervare
                      (date, sumă, proprietate) rămân, în scop contabil. Această acțiune nu poate
                      fi anulată.
                    </AlertDialogDescription>
                  </AlertDialogHeader>

                  <div className="flex flex-col divide-y divide-border/60 rounded-lg border text-sm">
                    {results.map((match) => (
                      <div key={`${match.recordType}-${match.id}`} className="flex items-center justify-between gap-3 p-2.5">
                        <span>
                          <Badge variant="outline" className="mr-2">{RECORD_TYPE_LABELS[match.recordType]}</Badge>
                          {match.name}
                        </span>
                        <span className="text-xs text-muted-foreground">{match.context}</span>
                      </div>
                    ))}
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs text-muted-foreground">
                      Tastează exact <span className="font-medium text-foreground">{searchedEmail}</span> ca să confirmi
                    </label>
                    <Input
                      value={confirmEmailInput}
                      onChange={(e) => setConfirmEmailInput(e.target.value)}
                      placeholder={searchedEmail}
                    />
                  </div>

                  <AlertDialogFooter>
                    <AlertDialogCancel onClick={() => setConfirmEmailInput("")}>Anulează</AlertDialogCancel>
                    <AlertDialogAction onClick={handleErase} disabled={!canConfirmErase || eraseData.isPending}>
                      Anonimizează definitiv
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
