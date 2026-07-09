"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { CheckCircle2, XCircle } from "lucide-react"
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
import { useCreatePublicBooking, usePublicAvailability } from "@/hooks/use-public-booking"
import type { PublicBookingPayload } from "@/lib/api/public"
import type { PublicPropertyResponse, PublicReservationResponse } from "@/lib/api/types"

const bookingSchema = z.object({
  guestFirstName: z.string().min(1, "Prenumele este obligatoriu"),
  guestLastName: z.string().min(1, "Numele este obligatoriu"),
  guestEmail: z.string().min(1, "Emailul este obligatoriu").email("Adresă de email invalidă"),
  guestPhone: z.string().min(1, "Telefonul este obligatoriu"),
  checkInDate: z.string().min(1, "Data de check-in este obligatorie"),
  checkOutDate: z.string().min(1, "Data de check-out este obligatorie"),
  numberOfGuests: z.coerce.number().int().min(1),
  notes: z.string().optional(),
})

interface BookingFormProps {
  property: PublicPropertyResponse
  defaultCheckIn?: string
  defaultCheckOut?: string
  defaultGuests?: number
  onSuccess: (reservation: PublicReservationResponse) => void
}

function nights(checkIn: string, checkOut: string) {
  if (!checkIn || !checkOut) return 0
  const diff = new Date(checkOut).getTime() - new Date(checkIn).getTime()
  return Math.max(0, Math.round(diff / 86400000))
}

export function BookingForm({
  property,
  defaultCheckIn,
  defaultCheckOut,
  defaultGuests,
  onSuccess,
}: BookingFormProps) {
  const createBooking = useCreatePublicBooking()

  const form = useForm({
    resolver: zodResolver(bookingSchema),
    defaultValues: {
      guestFirstName: "",
      guestLastName: "",
      guestEmail: "",
      guestPhone: "",
      checkInDate: defaultCheckIn ?? "",
      checkOutDate: defaultCheckOut ?? "",
      numberOfGuests: defaultGuests ?? Math.min(2, property.maxGuests),
      notes: "",
    },
  })

  const [checkInDate, checkOutDate] = form.watch(["checkInDate", "checkOutDate"])
  const { data: availability, isFetching: isCheckingAvailability } = usePublicAvailability(
    property.id,
    checkInDate,
    checkOutDate
  )

  const stayNights = nights(checkInDate, checkOutDate)
  const totalPrice =
    property.basePricePerNight != null ? property.basePricePerNight * stayNights : null

  function handleSubmit(values: z.infer<typeof bookingSchema>) {
    const payload: PublicBookingPayload = { ...values, propertyId: property.id }
    createBooking.mutate(payload, { onSuccess })
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="flex flex-col gap-4">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Perioadă & oaspeți</CardTitle>
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
            <FormField
              control={form.control}
              name="numberOfGuests"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Oaspeți (max {property.maxGuests})</FormLabel>
                  <FormControl>
                    <Input type="number" min={1} max={property.maxGuests} {...field} value={field.value as number} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {checkInDate && checkOutDate && !isCheckingAvailability && availability && (
              <div className="sm:col-span-2">
                {availability.available ? (
                  <p className="flex items-center gap-1.5 text-sm text-emerald-600 dark:text-emerald-400">
                    <CheckCircle2 className="size-4" />
                    Disponibil — {stayNights} nopți
                    {totalPrice != null && (
                      <span className="ml-1 font-medium text-foreground">
                        · {totalPrice.toLocaleString("ro-RO")} {property.currency}
                      </span>
                    )}
                  </p>
                ) : (
                  <p className="flex items-center gap-1.5 text-sm text-destructive">
                    <XCircle className="size-4" />
                    Indisponibil în perioada selectată.
                  </p>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Datele tale de contact</CardTitle>
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
              name="notes"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <FormLabel>Mesaj (opțional)</FormLabel>
                  <FormControl>
                    <Textarea rows={3} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Button type="submit" size="lg" disabled={createBooking.isPending}>
          {createBooking.isPending ? "Se trimite..." : "Trimite cererea de rezervare"}
        </Button>
      </form>
    </Form>
  )
}
