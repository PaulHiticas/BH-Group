"use client"

import { use, useRef, useState } from "react"
import Link from "next/link"
import {
  Calendar,
  Dices,
  KeyRound,
  Mail,
  Pencil,
  Phone,
  Send,
  Trash2,
  Users,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { buttonVariants, Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
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
import { ReservationStatusActions } from "@/components/reservations/reservation-status-actions"
import { ReservationPayments } from "@/components/reservations/reservation-payments"
import { MessageThread } from "@/components/messaging/message-thread"
import { useCurrentUser } from "@/hooks/use-current-user"
import { useReservationMessages, useSendStaffMessage } from "@/hooks/use-messages"
import {
  useDeleteReservation,
  useReservation,
  useSendCheckinInstructions,
  useUpdateAccessCode,
} from "@/hooks/use-reservations"
import {
  RESERVATION_SOURCE_LABELS,
  RESERVATION_STATUS_BADGE_VARIANT,
  RESERVATION_STATUS_LABELS,
} from "@/lib/reservation-labels"
import { cn } from "@/lib/utils"

export default function ReservationDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: reservation, isLoading } = useReservation(id)
  const { data: user } = useCurrentUser()
  const deleteReservation = useDeleteReservation()
  const updateAccessCode = useUpdateAccessCode(id)
  const sendCheckinInstructions = useSendCheckinInstructions(id)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const accessCodeRef = useRef<HTMLInputElement>(null)

  const isFullAdmin = user?.role === "SUPER_ADMIN" || user?.role === "ADMINISTRATOR"
  const canManage = isFullAdmin || user?.role === "SUPPORT_AGENT"
  const canDelete = isFullAdmin
  const canManagePayments = isFullAdmin || user?.role === "ACCOUNTANT"

  const { data: messages, isLoading: messagesLoading } = useReservationMessages(id, canManage)
  const sendStaffMessage = useSendStaffMessage(id)

  if (isLoading || !reservation) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  const canEdit =
    canManage && reservation.status !== "CHECKED_OUT" && reservation.status !== "CANCELLED"

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">
              {reservation.guestFirstName} {reservation.guestLastName}
            </h1>
            <Badge variant={RESERVATION_STATUS_BADGE_VARIANT[reservation.status]}>
              {RESERVATION_STATUS_LABELS[reservation.status]}
            </Badge>
          </div>
          <Link
            href={`/dashboard/properties/${reservation.propertyId}`}
            className="mt-1 text-sm text-muted-foreground hover:text-primary"
          >
            {reservation.propertyName}
          </Link>
        </div>

        {(canManage || canDelete) && (
          <div className="flex gap-2">
            {canEdit && (
              <Link
                href={`/dashboard/reservations/${id}/edit`}
                className={cn(buttonVariants({ variant: "outline" }))}
              >
                <Pencil className="size-4" />
                Editează
              </Link>
            )}
            {canDelete && (
              <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
                <AlertDialogTrigger render={<Button variant="destructive" />}>
                  <Trash2 className="size-4" />
                  Șterge
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Ștergi această rezervare?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Acțiunea este ireversibilă.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Anulează</AlertDialogCancel>
                    <AlertDialogAction onClick={() => deleteReservation.mutate(id)}>
                      Șterge definitiv
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            )}
          </div>
        )}
      </div>

      {canManage && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Acțiuni</CardTitle>
          </CardHeader>
          <CardContent>
            <ReservationStatusActions reservation={reservation} />
          </CardContent>
        </Card>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center gap-2 text-sm">
            <Calendar className="size-4 text-muted-foreground" />
            {reservation.checkInDate} → {reservation.checkOutDate}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-2 text-sm">
            <Users className="size-4 text-muted-foreground" />
            {reservation.numberOfGuests} oaspeți
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-sm">
            {RESERVATION_SOURCE_LABELS[reservation.source]}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Contact oaspete</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          {reservation.guestEmail && (
            <p className="flex items-center gap-2">
              <Mail className="size-4 text-muted-foreground" />
              {reservation.guestEmail}
            </p>
          )}
          {reservation.guestPhone && (
            <p className="flex items-center gap-2">
              <Phone className="size-4 text-muted-foreground" />
              {reservation.guestPhone}
            </p>
          )}
          {!reservation.guestEmail && !reservation.guestPhone && (
            <p className="text-muted-foreground">Nu există date de contact.</p>
          )}
        </CardContent>
      </Card>

      {canManage && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Acces (cod ușă / lockbox)</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="flex gap-2">
              <Input
                ref={accessCodeRef}
                key={reservation.accessCode ?? "empty"}
                defaultValue={reservation.accessCode ?? ""}
                placeholder="ex: 4471"
                className="max-w-40"
              />
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  if (accessCodeRef.current) {
                    accessCodeRef.current.value = String(Math.floor(1000 + Math.random() * 9000))
                  }
                }}
              >
                <Dices className="size-4" />
                Generează cod
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={updateAccessCode.isPending}
                onClick={() => updateAccessCode.mutate(accessCodeRef.current?.value ?? "")}
              >
                <KeyRound className="size-4" />
                Salvează cod
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={
                  sendCheckinInstructions.isPending ||
                  !reservation.accessCode ||
                  !reservation.guestEmail
                }
                onClick={() => sendCheckinInstructions.mutate()}
              >
                <Send className="size-4" />
                Trimite acum
              </Button>
            </div>
            <p className="text-xs text-muted-foreground">
              {reservation.accessCodeSentAt
                ? `Instrucțiuni trimise oaspetelui la ${new Date(reservation.accessCodeSentAt).toLocaleString("ro-RO")}.`
                : "Dacă setezi un cod, îl trimitem automat oaspetelui cu o zi înainte de check-in — sau apasă „Trimite acum”."}{" "}
              Recomandat: folosește un cod diferit la fiecare rezervare și schimbă-l fizic pe
              încuietoare/lockbox după fiecare oaspete.
            </p>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Plăți</CardTitle>
        </CardHeader>
        <CardContent>
          <ReservationPayments
            reservationId={id}
            totalAmount={reservation.totalAmount}
            currency={reservation.currency}
            canManage={canManagePayments}
          />
        </CardContent>
      </Card>

      {canManage && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Mesaje cu oaspetele</CardTitle>
          </CardHeader>
          <CardContent>
            <MessageThread
              messages={messages}
              isLoading={messagesLoading}
              viewerType="STAFF"
              onSend={(body) => sendStaffMessage.mutate(body)}
              isSending={sendStaffMessage.isPending}
              placeholder="Scrie un mesaj oaspetelui..."
            />
          </CardContent>
        </Card>
      )}

      {reservation.notes && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Note</CardTitle>
          </CardHeader>
          <CardContent className="whitespace-pre-line text-sm text-muted-foreground">
            {reservation.notes}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
