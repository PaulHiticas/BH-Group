"use client"

import { useState } from "react"
import { Plus } from "lucide-react"
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
import { DataPagination } from "@/components/ui/data-pagination"
import {
  useAssignMaintenanceTicket,
  useCreateMaintenanceTicket,
  useMaintenanceTickets,
  useUpdateMaintenanceTicketStatus,
} from "@/hooks/use-maintenance-tickets"
import { useProperties } from "@/hooks/use-properties"
import { useUsers } from "@/hooks/use-users"
import {
  ALL_MAINTENANCE_CATEGORIES,
  ALL_MAINTENANCE_PRIORITIES,
  ALL_MAINTENANCE_STATUSES,
  MAINTENANCE_CATEGORY_LABELS,
  MAINTENANCE_PRIORITY_BADGE_VARIANT,
  MAINTENANCE_PRIORITY_LABELS,
  MAINTENANCE_STATUS_BADGE_VARIANT,
  MAINTENANCE_STATUS_LABELS,
  nextMaintenanceStatuses,
} from "@/lib/cleaning-labels"
import type { MaintenanceCategory, MaintenancePriority, MaintenanceStatus } from "@/lib/api/types"

export function MaintenanceTicketsView() {
  const [status, setStatus] = useState<MaintenanceStatus | "ALL">("ALL")
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)

  const { data, isLoading } = useMaintenanceTickets({
    status: status === "ALL" ? undefined : status,
    page,
  })
  const { data: technicians } = useUsers({ role: "MAINTENANCE", size: 100 })
  const assignTechnician = useAssignMaintenanceTicket()
  const updateStatus = useUpdateMaintenanceTicketStatus()

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Mentenanță</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Probleme raportate la proprietăți. O problemă critică blochează automat rezervările noi.
          </p>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          <Plus className="size-4" />
          Raportează problemă
        </Button>
      </div>

      {showForm && <CreateTicketForm onCreated={() => setShowForm(false)} />}

      <Select
        value={status}
        onValueChange={(v) => {
          setStatus(v as MaintenanceStatus | "ALL")
          setPage(0)
        }}
      >
        <SelectTrigger className="w-48">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">Toate statusurile</SelectItem>
          {ALL_MAINTENANCE_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              {MAINTENANCE_STATUS_LABELS[s]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Niciun tichet de mentenanță.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((ticket) => {
              const nextStatuses = nextMaintenanceStatuses(ticket.status)
              return (
                <div key={ticket.id} className="flex flex-wrap items-center justify-between gap-3 p-4 text-sm">
                  <div>
                    <p className="font-medium">{ticket.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {ticket.propertyName} · {MAINTENANCE_CATEGORY_LABELS[ticket.category]}
                    </p>
                  </div>

                  <Badge variant={MAINTENANCE_PRIORITY_BADGE_VARIANT[ticket.priority]}>
                    {MAINTENANCE_PRIORITY_LABELS[ticket.priority]}
                  </Badge>

                  <div className="flex flex-col items-start gap-1">
                    <span className="text-xs text-muted-foreground">
                      {ticket.assignedToName ?? "Neasignat"}
                    </span>
                    <Select
                      value=""
                      onValueChange={(v) => {
                        if (v) assignTechnician.mutate({ id: ticket.id, assignedToId: v })
                      }}
                    >
                      <SelectTrigger className="w-48">
                        <SelectValue placeholder={ticket.assignedToId ? "Realocă" : "Alocă tehnician"} />
                      </SelectTrigger>
                      <SelectContent>
                        {technicians?.content.map((tech) => (
                          <SelectItem key={tech.id} value={tech.id}>
                            {tech.firstName} {tech.lastName}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <Badge variant={MAINTENANCE_STATUS_BADGE_VARIANT[ticket.status]}>
                    {MAINTENANCE_STATUS_LABELS[ticket.status]}
                  </Badge>

                  {nextStatuses.length > 0 && (
                    <div className="flex gap-1.5">
                      {nextStatuses.map((next) => (
                        <Button
                          key={next}
                          size="sm"
                          variant="outline"
                          onClick={() => updateStatus.mutate({ id: ticket.id, status: next })}
                        >
                          {MAINTENANCE_STATUS_LABELS[next]}
                        </Button>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}

function CreateTicketForm({ onCreated }: { onCreated: () => void }) {
  const { data: properties } = useProperties({ size: 100 })
  const createTicket = useCreateMaintenanceTicket()

  const [propertyId, setPropertyId] = useState("")
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [category, setCategory] = useState<MaintenanceCategory>("OTHER")
  const [priority, setPriority] = useState<MaintenancePriority>("MEDIUM")

  function handleSubmit() {
    if (!propertyId || !title) return
    createTicket.mutate(
      { propertyId, title, description: description || undefined, category, priority },
      { onSuccess: onCreated }
    )
  }

  return (
    <div className="flex flex-col gap-3 rounded-lg border p-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <Select value={propertyId} onValueChange={(v) => setPropertyId(v ?? "")}>
          <SelectTrigger>
            <SelectValue placeholder="Proprietate" />
          </SelectTrigger>
          <SelectContent>
            {properties?.content.map((property) => (
              <SelectItem key={property.id} value={property.id}>
                {property.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input placeholder="Titlu problemă" value={title} onChange={(e) => setTitle(e.target.value)} />
        <Select value={category} onValueChange={(v) => setCategory(v as MaintenanceCategory)}>
          <SelectTrigger>
            <SelectValue>{() => MAINTENANCE_CATEGORY_LABELS[category]}</SelectValue>
          </SelectTrigger>
          <SelectContent>
            {ALL_MAINTENANCE_CATEGORIES.map((c) => (
              <SelectItem key={c} value={c}>
                {MAINTENANCE_CATEGORY_LABELS[c]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={priority} onValueChange={(v) => setPriority(v as MaintenancePriority)}>
          <SelectTrigger>
            <SelectValue>{() => MAINTENANCE_PRIORITY_LABELS[priority]}</SelectValue>
          </SelectTrigger>
          <SelectContent>
            {ALL_MAINTENANCE_PRIORITIES.map((p) => (
              <SelectItem key={p} value={p}>
                {MAINTENANCE_PRIORITY_LABELS[p]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input
          placeholder="Descriere (opțional)"
          className="sm:col-span-2"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <Button
        className="self-start"
        disabled={!propertyId || !title || createTicket.isPending}
        onClick={handleSubmit}
      >
        Creează tichetul
      </Button>
    </div>
  )
}
