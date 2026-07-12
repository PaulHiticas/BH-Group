"use client"

import { use, useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Calendar, MapPin, Users, XCircle } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
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
} from "@/components/ui/alert-dialog"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { MessageThread } from "@/components/messaging/message-thread"
import { useBookingMessages, useSendGuestMessage } from "@/hooks/use-messages"
import {
  useBookingByToken,
  useBookingCancellationQuote,
  useCancelBookingByToken,
  useUpdateBookingByToken,
} from "@/hooks/use-public-booking"
import {
  RESERVATION_STATUS_BADGE_VARIANT,
  RESERVATION_STATUS_LABELS,
} from "@/lib/reservation-labels"

const updateSchema = z.object({
  checkInDate: z.string().min(1, "Data de check-in este obligatorie"),
  checkOutDate: z.string().min(1, "Data de check-out este obligatorie"),
  numberOfGuests: z.coerce.number().int().min(1),
})

export default function ManageBookingPage({
  params,
}: {
  params: Promise<{ token: string }>
}) {
  const { token } = use(params)
  const { data: reservation, isLoading, isError } = useBookingByToken(token)
  const updateBooking = useUpdateBookingByToken(token)
  const cancelBooking = useCancelBookingByToken(token)
  const [editing, setEditing] = useState(false)
  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false)
  const { data: cancellationQuote } = useBookingCancellationQuote(token, confirmCancelOpen)
  const { data: messages, isLoading: messagesLoading } = useBookingMessages(token)
  const sendGuestMessage = useSendGuestMessage(token)

  const form = useForm({
    resolver: zodResolver(updateSchema),
    values: reservation
      ? {
          checkInDate: reservation.checkInDate,
          checkOutDate: reservation.checkOutDate,
          numberOfGuests: reservation.numberOfGuests,
        }
      : undefined,
  })

  if (isLoading) {
    return (
      <div className="mx-auto flex max-w-lg flex-col gap-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (isError || !reservation) {
    return (
      <div className="mx-auto flex max-w-lg flex-col items-center gap-3 text-center">
        <XCircle className="size-10 text-destructive" />
        <h1 className="text-xl font-semibold">Rezervare negăsită</h1>
        <p className="text-sm text-muted-foreground">
          Link-ul de gestionare este invalid sau a expirat.
        </p>
      </div>
    )
  }

  const canModify = reservation.status === "PENDING" || reservation.status === "CONFIRMED"

  function handleUpdate(values: z.infer<typeof updateSchema>) {
    updateBooking.mutate(values, { onSuccess: () => setEditing(false) })
  }

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-6">
      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold tracking-tight">Rezervarea ta</h1>
          <Badge variant={RESERVATION_STATUS_BADGE_VARIANT[reservation.status]}>
            {RESERVATION_STATUS_LABELS[reservation.status]}
          </Badge>
        </div>
        <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
          <MapPin className="size-3.5" />
          {reservation.propertyName}, {reservation.propertyCity}
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-3 text-sm">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-2 text-muted-foreground">
              <Calendar className="size-4" />
              Perioadă
            </span>
            <span className="font-medium">
              {reservation.checkInDate} → {reservation.checkOutDate}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-2 text-muted-foreground">
              <Users className="size-4" />
              Oaspeți
            </span>
            <span className="font-medium">{reservation.numberOfGuests}</span>
          </div>
          {reservation.totalAmount != null && (
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Sumă totală</span>
              <span className="font-medium">
                {reservation.totalAmount} {reservation.currency}
              </span>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Mesaje cu echipa BH Group</CardTitle>
        </CardHeader>
        <CardContent>
          <MessageThread
            messages={messages}
            isLoading={messagesLoading}
            viewerType="GUEST"
            onSend={(body) => sendGuestMessage.mutate(body)}
            isSending={sendGuestMessage.isPending}
            placeholder="Scrie un mesaj echipei..."
          />
        </CardContent>
      </Card>

      {canModify && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">
              {editing ? "Modifică rezervarea" : "Acțiuni"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {editing ? (
              <Form {...form}>
                <form onSubmit={form.handleSubmit(handleUpdate)} className="flex flex-col gap-4">
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={form.control}
                      name="checkInDate"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Check-in</FormLabel>
                          <FormControl>
                            <Input type="date" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="checkOutDate"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Check-out</FormLabel>
                          <FormControl>
                            <Input type="date" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <FormField
                    control={form.control}
                    name="numberOfGuests"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Oaspeți</FormLabel>
                        <FormControl>
                          <Input type="number" min={1} {...field} value={field.value as number} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="flex gap-2">
                    <Button type="submit" disabled={updateBooking.isPending}>
                      {updateBooking.isPending ? "Se salvează..." : "Salvează"}
                    </Button>
                    <Button type="button" variant="outline" onClick={() => setEditing(false)}>
                      Anulează
                    </Button>
                  </div>
                </form>
              </Form>
            ) : (
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => setEditing(true)}>
                  Modifică perioada
                </Button>
                <AlertDialog open={confirmCancelOpen} onOpenChange={setConfirmCancelOpen}>
                  <Button variant="destructive" onClick={() => setConfirmCancelOpen(true)}>
                    Anulează rezervarea
                  </Button>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Anulezi această rezervare?</AlertDialogTitle>
                      <AlertDialogDescription>
                        Acțiunea este ireversibilă. Va trebui să faci o rezervare nouă dacă te răzgândești.
                        {cancellationQuote && (
                          <>
                            {" "}
                            {cancellationQuote.estimatedRefundAmount > 0 ? (
                              <>
                                Conform politicii de anulare, vei primi înapoi{" "}
                                <strong>
                                  {cancellationQuote.estimatedRefundAmount} {cancellationQuote.currency} (
                                  {cancellationQuote.refundPercent}%)
                                </strong>
                                .
                              </>
                            ) : (
                              "Conform politicii de anulare, suma plătită nu este rambursabilă în acest moment."
                            )}
                          </>
                        )}
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Înapoi</AlertDialogCancel>
                      <AlertDialogAction onClick={() => cancelBooking.mutate()}>
                        Da, anulează
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
