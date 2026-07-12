"use client"

import { useRef, useState } from "react"
import { FileText, Trash2, Upload } from "lucide-react"
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
import type { PropertyDocumentResponse, PropertyDocumentType } from "@/lib/api/types"
import { PROPERTY_DOCUMENT_TYPE_LABELS } from "@/lib/property-labels"
import {
  useDeletePropertyDocument,
  useUploadPropertyDocument,
} from "@/hooks/use-properties"

const DOCUMENT_TYPES: PropertyDocumentType[] = [
  "CONTRACT",
  "INVOICE",
  "ID_COPY",
  "UTILITY_BILL",
  "OTHER",
]

const EXPIRY_WARNING_DAYS = 30

function expiryBadge(expiresAt: string | null) {
  if (!expiresAt) return null
  const days = Math.ceil(
    (new Date(expiresAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24)
  )
  if (days < 0) {
    return <Badge variant="destructive">Expirat</Badge>
  }
  if (days <= EXPIRY_WARNING_DAYS) {
    return <Badge variant="secondary">Expiră în {days} zile</Badge>
  }
  return (
    <span className="text-xs text-muted-foreground">
      Expiră la {new Date(expiresAt).toLocaleDateString("ro-RO")}
    </span>
  )
}

export function DocumentList({
  propertyId,
  documents,
  canManage,
}: {
  propertyId: string
  documents: PropertyDocumentResponse[]
  canManage: boolean
}) {
  const [documentType, setDocumentType] = useState<PropertyDocumentType>("OTHER")
  const [expiresAt, setExpiresAt] = useState("")
  const fileInputRef = useRef<HTMLInputElement>(null)
  const uploadDocument = useUploadPropertyDocument(propertyId)
  const deleteDocument = useDeletePropertyDocument(propertyId)

  function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) {
      uploadDocument.mutate(
        { file, documentType, expiresAt: expiresAt || undefined },
        { onSuccess: () => setExpiresAt("") }
      )
    }
    e.target.value = ""
  }

  return (
    <div className="flex flex-col gap-3">
      {documents.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nu există documente încă.</p>
      ) : (
        <div className="flex flex-col divide-y divide-border/60">
          {documents.map((document) => (
            <div key={document.id} className="flex flex-wrap items-center justify-between gap-2 py-2.5">
              <a
                href={document.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 text-sm hover:text-primary"
              >
                <FileText className="size-4 text-muted-foreground" />
                {document.fileName}
                <span className="text-xs text-muted-foreground">
                  ({PROPERTY_DOCUMENT_TYPE_LABELS[document.documentType]})
                </span>
              </a>
              <div className="flex items-center gap-2">
                {expiryBadge(document.expiresAt)}
                {canManage && (
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    onClick={() => deleteDocument.mutate(document.id)}
                    aria-label="Șterge document"
                  >
                    <Trash2 className="size-4 text-destructive" />
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {canManage && (
        <div className="flex flex-wrap items-center gap-2 pt-2">
          <Select value={documentType} onValueChange={(v) => setDocumentType(v as PropertyDocumentType)}>
            <SelectTrigger className="w-44">
              <SelectValue>{() => PROPERTY_DOCUMENT_TYPE_LABELS[documentType]}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              {DOCUMENT_TYPES.map((type) => (
                <SelectItem key={type} value={type}>
                  {PROPERTY_DOCUMENT_TYPE_LABELS[type]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            type="date"
            value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)}
            className="w-40"
            aria-label="Dată expirare (opțional)"
            title="Dată expirare (opțional)"
          />
          <Button
            variant="outline"
            size="sm"
            disabled={uploadDocument.isPending}
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="size-4" />
            {uploadDocument.isPending ? "Se încarcă..." : "Încarcă document"}
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept="application/pdf,image/jpeg,image/png,.doc,.docx"
            className="hidden"
            onChange={handleFileSelected}
          />
        </div>
      )}
    </div>
  )
}
