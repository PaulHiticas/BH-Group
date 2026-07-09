"use client"

import { useRef, useState } from "react"
import { FileText, Trash2, Upload } from "lucide-react"
import { Button } from "@/components/ui/button"
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
  const fileInputRef = useRef<HTMLInputElement>(null)
  const uploadDocument = useUploadPropertyDocument(propertyId)
  const deleteDocument = useDeletePropertyDocument(propertyId)

  function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) {
      uploadDocument.mutate({ file, documentType })
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
            <div key={document.id} className="flex items-center justify-between py-2.5">
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
          ))}
        </div>
      )}

      {canManage && (
        <div className="flex items-center gap-2 pt-2">
          <Select value={documentType} onValueChange={(v) => setDocumentType(v as PropertyDocumentType)}>
            <SelectTrigger className="w-44">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {DOCUMENT_TYPES.map((type) => (
                <SelectItem key={type} value={type}>
                  {PROPERTY_DOCUMENT_TYPE_LABELS[type]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
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
