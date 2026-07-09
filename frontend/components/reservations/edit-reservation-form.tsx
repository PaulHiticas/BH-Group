"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { ALL_RESERVATION_SOURCES, RESERVATION_SOURCE_LABELS } from "@/lib/reservation-labels"
import type { ReservationPayload } from "@/lib/api/reservations"
import type { ReservationResponse } from "@/lib/api/types"

const reservationSchema = z.object({
  guestFirstName: z.string().min(1, "Prenumele oaspetelui este obligatoriu"),
  guestLastName: z.string().min(1, "Numele oaspetelui este obligatoriu"),
  guestEmail: z.string().email("Adresă de email invalidă").optional().or(z.literal("")),
  guestPhone: z.string().optional(),
  checkInDate: z.string().min(1, "Data de check-in este obligatorie"),
  checkOutDate: z.string().min(1, "Data de check-out este obligatorie"),
  numberOfGuests: z.coerce.number().int().min(1),
  source: z.enum(ALL_RESERVATION_SOURCES as [string, ...string[]]),
  totalAmount: z.coerce.number().min(0).optional(),
  currency: z.string().min(1).max(3),
  notes: z.string().optional(),
})

interface EditReservationFormProps {
  reservation: ReservationResponse
  onSubmit: (values: ReservationPayload) => void
  isSubmitting?: boolean
}

export function EditReservationForm({ reservation, onSubmit, isSubmitting }: EditReservationFormProps) {
  const form = useForm({
    resolver: zodResolver(reservationSchema),
    defaultValues: {
      guestFirstName: reservation.guestFirstName,
      guestLastName: reservation.guestLastName,
      guestEmail: reservation.guestEmail ?? "",
      guestPhone: reservation.guestPhone ?? "",
      checkInDate: reservation.checkInDate,
      checkOutDate: reservation.checkOutDate,
      numberOfGuests: reservation.numberOfGuests,
      source: reservation.source,
      totalAmount: reservation.totalAmount ?? undefined,
      currency: reservation.currency,
      notes: reservation.notes ?? "",
    },
  })

  function handleFormSubmit(values: z.infer<typeof reservationSchema>) {
    onSubmit({
      ...values,
      guestEmail: values.guestEmail || undefined,
      source: values.source as ReservationPayload["source"],
    })
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleFormSubmit)} className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Perioadă</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
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
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Detalii oaspete</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="guestFirstName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Prenume</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="guestLastName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nume</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="guestEmail"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input type="email" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="guestPhone"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Telefon</FormLabel>
                  <FormControl>
                    <Input type="tel" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="numberOfGuests"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Număr oaspeți</FormLabel>
                  <FormControl>
                    <Input type="number" min={1} {...field} value={field.value as number} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="source"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Sursă rezervare</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {ALL_RESERVATION_SOURCES.map((source) => (
                        <SelectItem key={source} value={source}>
                          {RESERVATION_SOURCE_LABELS[source]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Plată & note</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="totalAmount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Sumă totală</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="0.01"
                      {...field}
                      value={(field.value as number | undefined) ?? ""}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="currency"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Monedă</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <FormLabel>Note</FormLabel>
                  <FormControl>
                    <Textarea rows={3} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <div className="flex justify-end">
          <Button type="submit" size="lg" disabled={isSubmitting}>
            {isSubmitting ? "Se salvează..." : "Salvează modificările"}
          </Button>
        </div>
      </form>
    </Form>
  )
}
