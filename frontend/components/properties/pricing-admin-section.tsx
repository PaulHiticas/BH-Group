"use client"

import { useEffect, useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Plus, Trash2 } from "lucide-react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
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
import type { DynamicPricingConfigResponse } from "@/lib/api/pricing"
import {
  useCreateLocalEvent,
  useDeleteLocalEvent,
  useMergedLocalEvents,
  usePricingBreakdown,
  usePricingConfig,
  useUpdatePricingConfig,
} from "@/hooks/use-pricing"

function formatMoney(value: number | null, currency: string) {
  if (value == null) return "—"
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 2 }).format(value) + " " + currency
}

function formatFactor(value: number) {
  return value.toFixed(2)
}

export const dynamicPricingConfigSchema = z
  .object({
    enabled: z.boolean(),
    minPrice: z.number({ error: "Introduceți un număr" }).min(0, "Nu poate fi negativ").nullable(),
    maxPrice: z.number({ error: "Introduceți un număr" }).min(0, "Nu poate fi negativ").nullable(),
    occupancyWindowDays: z
      .number({ error: "Introduceți un număr" })
      .int("Trebuie să fie număr întreg")
      .positive("Trebuie să fie mai mare ca 0"),
    occupancyMultiplierMin: z
      .number({ error: "Introduceți un număr" })
      .positive("Trebuie să fie mai mare ca 0"),
    occupancyMultiplierMax: z
      .number({ error: "Introduceți un număr" })
      .positive("Trebuie să fie mai mare ca 0"),
    leadTimeDays: z
      .number({ error: "Introduceți un număr" })
      .int("Trebuie să fie număr întreg")
      .min(0, "Nu poate fi negativ"),
    leadTimeMultiplier: z
      .number({ error: "Introduceți un număr" })
      .positive("Trebuie să fie mai mare ca 0"),
  })
  .superRefine((data, ctx) => {
    if (data.minPrice != null && data.maxPrice != null && data.minPrice > data.maxPrice) {
      ctx.addIssue({
        code: "custom",
        message: "Prețul minim nu poate fi mai mare decât maximul",
        path: ["minPrice"],
      })
    }
    if (data.occupancyMultiplierMax < data.occupancyMultiplierMin) {
      ctx.addIssue({
        code: "custom",
        message: "Multiplicatorul maxim nu poate fi mai mic decât minimul",
        path: ["occupancyMultiplierMax"],
      })
    }
  })

type ConfigFormValues = z.infer<typeof dynamicPricingConfigSchema>

const DEFAULT_CONFIG_VALUES: ConfigFormValues = {
  enabled: false,
  minPrice: null,
  maxPrice: null,
  occupancyWindowDays: 14,
  occupancyMultiplierMin: 0.9,
  occupancyMultiplierMax: 1.3,
  leadTimeDays: 7,
  leadTimeMultiplier: 1,
}

function toFormValues(config: DynamicPricingConfigResponse): ConfigFormValues {
  return {
    enabled: config.enabled,
    minPrice: config.minPrice,
    maxPrice: config.maxPrice,
    occupancyWindowDays: config.occupancyWindowDays,
    occupancyMultiplierMin: config.occupancyMultiplierMin,
    occupancyMultiplierMax: config.occupancyMultiplierMax,
    leadTimeDays: config.leadTimeDays,
    leadTimeMultiplier: config.leadTimeMultiplier,
  }
}

function PricingConfigForm({ propertyId }: { propertyId: string }) {
  const { data: config, isLoading } = usePricingConfig(propertyId)
  const updateConfig = useUpdatePricingConfig(propertyId)

  const form = useForm<ConfigFormValues>({
    resolver: zodResolver(dynamicPricingConfigSchema),
    defaultValues: DEFAULT_CONFIG_VALUES,
  })

  useEffect(() => {
    if (config) form.reset(toFormValues(config))
  }, [config, form])

  function onSubmit(values: ConfigFormValues) {
    updateConfig.mutate(values)
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3">
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    )
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <FormField
          control={form.control}
          name="enabled"
          render={({ field }) => (
            <FormItem>
              <label className="flex items-center gap-2 text-sm font-medium">
                <FormControl>
                  <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                </FormControl>
                Preț dinamic activat
              </label>
              <FormDescription>
                Dezactivat = se folosește doar prețul standard/sezonier existent; niciunul din factorii de mai jos
                nu are efect.
              </FormDescription>
            </FormItem>
          )}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="minPrice"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Preț minim (plafon siguranță)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0}
                    step="0.01"
                    value={field.value ?? ""}
                    onChange={(e) => field.onChange(e.target.value === "" ? null : e.target.valueAsNumber)}
                  />
                </FormControl>
                <FormDescription>Gol = fără plafon minim. Prețul final nu va scădea sub această valoare.</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="maxPrice"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Preț maxim (plafon siguranță)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0}
                    step="0.01"
                    value={field.value ?? ""}
                    onChange={(e) => field.onChange(e.target.value === "" ? null : e.target.valueAsNumber)}
                  />
                </FormControl>
                <FormDescription>Gol = fără plafon maxim. Prețul final nu va depăși această valoare.</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <FormField
            control={form.control}
            name="occupancyWindowDays"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Fereastră ocupare (zile)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={1}
                    step="1"
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.valueAsNumber)}
                  />
                </FormControl>
                <FormDescription>Interval de ±N zile analizat în jurul fiecărei nopți.</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="occupancyMultiplierMin"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Multiplicator ocupare — minim</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0.01}
                    step="0.01"
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.valueAsNumber)}
                  />
                </FormControl>
                <FormDescription>Ex: 0.90 = până la -10% când fereastra e goală.</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="occupancyMultiplierMax"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Multiplicator ocupare — maxim</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0.01}
                    step="0.01"
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.valueAsNumber)}
                  />
                </FormControl>
                <FormDescription>Ex: 1.30 = până la +30% când fereastra e plină.</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="leadTimeDays"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Prag last-minute (zile până la check-in)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0}
                    step="1"
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.valueAsNumber)}
                  />
                </FormControl>
                <FormDescription>Sub acest număr de zile se aplică multiplicatorul de alături.</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="leadTimeMultiplier"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Multiplicator last-minute</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0.01}
                    step="0.01"
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.valueAsNumber)}
                  />
                </FormControl>
                <FormDescription>1.00 = fără efect.</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <Button type="submit" className="self-start" disabled={updateConfig.isPending}>
          {updateConfig.isPending ? "Se salvează..." : "Salvează configurația"}
        </Button>
      </form>
    </Form>
  )
}

function LocalEventsManager({ propertyId, city }: { propertyId: string; city: string }) {
  const { events, isLoading } = useMergedLocalEvents(propertyId, city)
  const createEvent = useCreateLocalEvent(propertyId, city)
  const deleteEvent = useDeleteLocalEvent(propertyId, city)

  const [label, setLabel] = useState("")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [priceMultiplier, setPriceMultiplier] = useState("")
  const [scope, setScope] = useState<"property" | "city">("property")

  const priceMultiplierNumber = priceMultiplier === "" ? null : Number(priceMultiplier)
  const dateOrderInvalid = !!startDate && !!endDate && endDate < startDate
  const multiplierInvalid = priceMultiplierNumber != null && !(priceMultiplierNumber > 0)
  const canSubmit =
    label.trim() !== "" &&
    startDate !== "" &&
    endDate !== "" &&
    !dateOrderInvalid &&
    priceMultiplierNumber != null &&
    !multiplierInvalid &&
    (scope === "property" || (scope === "city" && !!city))

  function handleAdd() {
    if (!canSubmit || priceMultiplierNumber == null) return
    createEvent.mutate(
      {
        label: label.trim(),
        startDate,
        endDate,
        priceMultiplier: priceMultiplierNumber,
        ...(scope === "property" ? { propertyId } : { city }),
      },
      {
        onSuccess: () => {
          setLabel("")
          setStartDate("")
          setEndDate("")
          setPriceMultiplier("")
          setScope("property")
        },
      }
    )
  }

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Se încarcă evenimentele...</p>
  }

  return (
    <div className="flex flex-col gap-3">
      {events.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Niciun eveniment definit pentru acest apartament sau pentru orașul {city || "necunoscut"}.
        </p>
      ) : (
        <div className="flex flex-col divide-y divide-border/60">
          {events.map((event) => (
            <div key={event.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-medium">{event.label}</span>
                  <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                    {event.propertyId ? "Acest apartament" : `Tot orașul ${event.city}`}
                  </span>
                </div>
                <span className="text-muted-foreground">
                  {event.startDate} → {event.endDate} · ×{formatFactor(event.priceMultiplier)}
                </span>
              </div>
              <AlertDialog>
                <AlertDialogTrigger render={<Button size="icon-sm" variant="ghost" aria-label="Șterge eveniment" />}>
                  <Trash2 className="size-4 text-destructive" />
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Ștergi evenimentul „{event.label}”?</AlertDialogTitle>
                    <AlertDialogDescription>Acțiunea este ireversibilă.</AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Anulează</AlertDialogCancel>
                    <AlertDialogAction onClick={() => deleteEvent.mutate(event.id)}>Șterge</AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </div>
          ))}
        </div>
      )}

      <div className="grid gap-2 pt-2 sm:grid-cols-6">
        <Input
          placeholder="Etichetă (ex: Festival local)"
          value={label}
          onChange={(e) => setLabel(e.target.value)}
          className="sm:col-span-2"
        />
        <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        <Input
          type="number"
          min={0.01}
          step="0.01"
          placeholder="Multiplicator"
          value={priceMultiplier}
          onChange={(e) => setPriceMultiplier(e.target.value)}
        />
        <div className="flex gap-2">
          <Select value={scope} onValueChange={(value) => setScope(value as "property" | "city")}>
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="property">Doar acest apartament</SelectItem>
              {city && <SelectItem value="city">Tot orașul {city}</SelectItem>}
            </SelectContent>
          </Select>
          <Button
            size="icon"
            variant="outline"
            disabled={!canSubmit || createEvent.isPending}
            onClick={handleAdd}
            aria-label="Adaugă eveniment"
          >
            <Plus className="size-4" />
          </Button>
        </div>
      </div>
      {dateOrderInvalid && (
        <p className="text-sm text-destructive">Data de sfârșit nu poate fi înainte de data de început.</p>
      )}
      {multiplierInvalid && <p className="text-sm text-destructive">Multiplicatorul trebuie să fie mai mare ca 0.</p>}
      {!city && <p className="text-sm text-muted-foreground">Orașul proprietății nu este setat — poți adăuga evenimente doar pentru acest apartament.</p>}
    </div>
  )
}

function BreakdownPreview({ propertyId }: { propertyId: string }) {
  const [checkIn, setCheckIn] = useState("")
  const [checkOut, setCheckOut] = useState("")
  const [guests, setGuests] = useState("1")

  const guestsNumber = Number(guests) || 0
  const { data: breakdown, isLoading, isFetching } = usePricingBreakdown(propertyId, {
    checkIn,
    checkOut,
    guests: guestsNumber,
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-2 sm:grid-cols-4">
        <div className="flex flex-col gap-1">
          <Label htmlFor="preview-check-in">Check-in</Label>
          <Input id="preview-check-in" type="date" value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="preview-check-out">Check-out</Label>
          <Input id="preview-check-out" type="date" value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="preview-guests">Oaspeți</Label>
          <Input
            id="preview-guests"
            type="number"
            min={1}
            value={guests}
            onChange={(e) => setGuests(e.target.value)}
          />
        </div>
      </div>

      {checkOut && checkIn && checkOut <= checkIn && (
        <p className="text-sm text-destructive">Data de check-out trebuie să fie după check-in.</p>
      )}

      {(isLoading || isFetching) && checkIn && checkOut && checkOut > checkIn && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      )}

      {breakdown && !breakdown.available && (
        <Alert variant="destructive">
          <AlertTitle>Indisponibil</AlertTitle>
          <AlertDescription>{breakdown.unavailableReason ?? "Nu există preț configurat pentru acest interval."}</AlertDescription>
        </Alert>
      )}

      {breakdown && breakdown.available && (
        <div className="flex flex-col gap-3">
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Noapte</TableHead>
                  <TableHead>Preț bază</TableHead>
                  <TableHead>Ocupare</TableHead>
                  <TableHead>Last-minute</TableHead>
                  <TableHead>Eveniment</TableHead>
                  <TableHead>Înainte de plafon</TableHead>
                  <TableHead>Preț final</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {breakdown.nightlyBreakdown.map((night) => (
                  <TableRow key={night.date}>
                    <TableCell>{night.date}</TableCell>
                    <TableCell>{formatMoney(night.baseRate, breakdown.currency)}</TableCell>
                    <TableCell>
                      ×{formatFactor(night.occupancyFactor)}
                      <span className="ml-1 text-xs text-muted-foreground">
                        ({night.commerciallyBookedNights}/{night.sellableNights})
                      </span>
                    </TableCell>
                    <TableCell>×{formatFactor(night.leadTimeFactor)}</TableCell>
                    <TableCell>×{formatFactor(night.eventFactor)}</TableCell>
                    <TableCell>{formatMoney(night.rateBeforeClamp, breakdown.currency)}</TableCell>
                    <TableCell className="font-medium">{formatMoney(night.rateAfterClamp, breakdown.currency)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="grid gap-2 rounded-lg border p-3 text-sm sm:grid-cols-2">
            <div>
              <span className="text-muted-foreground">Subtotal: </span>
              {formatMoney(breakdown.subtotal, breakdown.currency)}
            </div>
            <div>
              <span className="text-muted-foreground">Taxă oaspete suplimentar: </span>
              {formatMoney(breakdown.extraGuestFee, breakdown.currency)}
            </div>
            <div>
              <span className="text-muted-foreground">Taxă curățenie: </span>
              {formatMoney(breakdown.cleaningFee, breakdown.currency)}
            </div>
            <div>
              <span className="text-muted-foreground">Discount: </span>
              {breakdown.discountPercent != null
                ? `${breakdown.discountPercent}% (${formatMoney(breakdown.discountAmount, breakdown.currency)})`
                : "—"}
            </div>
            <div className="sm:col-span-2 text-base font-semibold">
              Total: {formatMoney(breakdown.totalAmount, breakdown.currency)}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export function PricingAdminSection({ propertyId, city }: { propertyId: string; city: string }) {
  return (
    <div className="flex flex-col gap-8">
      <div>
        <h3 className="mb-3 text-sm font-semibold text-muted-foreground">Configurare</h3>
        <PricingConfigForm propertyId={propertyId} />
      </div>

      <div>
        <h3 className="mb-3 text-sm font-semibold text-muted-foreground">Evenimente locale</h3>
        <LocalEventsManager propertyId={propertyId} city={city} />
      </div>

      <div>
        <h3 className="mb-3 text-sm font-semibold text-muted-foreground">Previzualizare preț pe noapte</h3>
        <BreakdownPreview propertyId={propertyId} />
      </div>
    </div>
  )
}
