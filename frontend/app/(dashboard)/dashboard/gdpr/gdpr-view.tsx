"use client"

import { useState } from "react"
import { toast } from "sonner"
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
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { downloadFile } from "@/lib/download-file"
import { useEraseGdprData, useGdprSearch } from "@/hooks/use-gdpr"
import type { GdprRecordType } from "@/lib/api/types"

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", { dateStyle: "medium", timeStyle: "short" })
}

const RECORD_TYPE_LABELS: Record<GdprRecordType, string> = {
  RESERVATION: "Rezervare",
  LEAD: "Lead",
}

export function GdprView() {
  const [email, setEmail] = useState("")
  const [searchedEmail, setSearchedEmail] = useState("")
  const { data: results, isLoading, isFetching, refetch, isFetched } = useGdprSearch(searchedEmail)
  const eraseData = useEraseGdprData()

  function handleSearch() {
    const trimmed = email.trim()
    if (!trimmed) return
    setSearchedEmail(trimmed)
    setTimeout(refetch, 0)
  }

  async function handleExport() {
    try {
      await downloadFile(
        `/admin/gdpr/export?email=${encodeURIComponent(searchedEmail)}`,
        `gdpr-export-${searchedEmail}.json`
      )
    } catch {
      toast.error("Exportul a eșuat")
    }
  }

  function handleErase() {
    eraseData.mutate(searchedEmail, { onSuccess: () => refetch() })
  }

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">GDPR — solicitări privind datele</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Caută toate rezervările și lead-urile asociate unei adrese de email, exportă-le sau
          anonimizează-le la cererea persoanei vizate. Rezervările nu sunt șterse (se păstrează
          datele/sumele în scop contabil), doar datele de identificare sunt eliminate.
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
        <Button type="button" disabled={!email.trim()} onClick={handleSearch}>
          Caută
        </Button>
      </div>

      {isLoading || isFetching ? (
        <Skeleton className="h-48 w-full" />
      ) : !isFetched ? null : !results || results.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">
            Nicio înregistrare găsită pentru <span className="font-medium">{searchedEmail}</span>.
          </p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-muted-foreground">
              {results.length} înregistrare(ări) găsite pentru <span className="font-medium">{searchedEmail}</span>
            </p>
            <div className="flex gap-2">
              <Button type="button" variant="outline" size="sm" className="gap-2" onClick={handleExport}>
                <Download className="size-4" />
                Exportă (JSON)
              </Button>
              <AlertDialog>
                <AlertDialogTrigger render={<Button variant="destructive" size="sm" className="gap-2" />}>
                  <ShieldAlert className="size-4" />
                  Anonimizează tot
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Anonimizezi toate datele pentru {searchedEmail}?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Numele, emailul, telefonul, notele și codurile de acces vor fi șterse ireversibil
                      din toate cele {results.length} înregistrări găsite. Datele de rezervare (date,
                      sumă, proprietate) rămân, în scop contabil. Această acțiune nu poate fi anulată.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Anulează</AlertDialogCancel>
                    <AlertDialogAction onClick={handleErase} disabled={eraseData.isPending}>
                      Anonimizează definitiv
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </div>
          </div>

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
        </div>
      )}
    </div>
  )
}
