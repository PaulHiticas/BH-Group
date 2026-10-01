"use client"

import { useEffect, useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Plus, RotateCcw, Sparkles, Trash2 } from "lucide-react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
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
import {
  aiRecommendationErrorMessage,
  type AiPricingConfidence,
  type AiPricingRecommendationResponse,
  type AiRecommendedPricingConfig,
  type DynamicPricingConfigResponse,
} from "@/lib/api/pricing"
import {
  useAiPricingRecommendation,
  useCreateLocalEvent,
  useDeleteLocalEvent,
  useMergedLocalEvents,
  usePricingBreakdown,
  usePricingConfig,
  useUpdatePricingConfig,
} from "@/hooks/use-pricing"
import { useCurrentUser } from "@/hooks/use-current-user"

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

type ConfigField = keyof ConfigFormValues

/** Order here is the order the comparison table is rendered in. */
const CONFIG_FIELD_LABELS: Record<ConfigField, string> = {
  enabled: "Preț dinamic",
  minPrice: "Preț minim",
  maxPrice: "Preț maxim",
  occupancyWindowDays: "Fereastră ocupare",
  occupancyMultiplierMin: "Multiplicator ocupare — minim",
  occupancyMultiplierMax: "Multiplicator ocupare — maxim",
  leadTimeDays: "Prag last-minute",
  leadTimeMultiplier: "Multiplicator last-minute",
}

const CONFIG_FIELDS = Object.keys(CONFIG_FIELD_LABELS) as ConfigField[]

const CONFIDENCE_LABELS: Record<AiPricingConfidence, string> = {
  LOW: "Scăzută",
  MEDIUM: "Medie",
  HIGH: "Ridicată",
}

const CONFIDENCE_VARIANTS: Record<AiPricingConfidence, "destructive" | "secondary" | "default"> = {
  LOW: "destructive",
  MEDIUM: "secondary",
  HIGH: "default",
}

/** How long a field changed by the AI stays visually marked. */
const AI_HIGHLIGHT_MS = 6000

function toFormValuesFromRecommendation(recommendation: AiRecommendedPricingConfig): ConfigFormValues {
  return {
    enabled: recommendation.enabled,
    minPrice: recommendation.minPrice,
    maxPrice: recommendation.maxPrice,
    occupancyWindowDays: recommendation.occupancyWindowDays,
    occupancyMultiplierMin: recommendation.occupancyMultiplierMin,
    occupancyMultiplierMax: recommendation.occupancyMultiplierMax,
    leadTimeDays: recommendation.leadTimeDays,
    leadTimeMultiplier: recommendation.leadTimeMultiplier,
  }
}

function formatConfigValue(field: ConfigField, values: ConfigFormValues, currency: string) {
  switch (field) {
    case "enabled":
      return values.enabled ? "Activat" : "Dezactivat"
    case "minPrice":
      return formatMoney(values.minPrice, currency)
    case "maxPrice":
      return formatMoney(values.maxPrice, currency)
    case "occupancyWindowDays":
      return `${values.occupancyWindowDays} zile`
    case "leadTimeDays":
      return `${values.leadTimeDays} zile`
    case "occupancyMultiplierMin":
      return `×${formatFactor(values.occupancyMultiplierMin)}`
    case "occupancyMultiplierMax":
      return `×${formatFactor(values.occupancyMultiplierMax)}`
    case "leadTimeMultiplier":
      return `×${formatFactor(values.leadTimeMultiplier)}`
  }
}

function formatPercent(rate: number) {
  return `${Math.round(rate * 100)}%`
}

function formatTimestamp(iso: string) {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return new Intl.DateTimeFormat("ro-RO", { dateStyle: "short", timeStyle: "short" }).format(date)
}

/**
 * What the AI suggested and why, alongside the values it would replace. Shown
 * only after a recommendation has been applied to the form, so the admin can
 * judge the change before it is saved - nothing here persists anything.
 */
function AiRecommendationCard({
  recommendation,
  previousValues,
  changedFields,
  onRevert,
}: {
  recommendation: AiPricingRecommendationResponse
  previousValues: ConfigFormValues
  changedFields: ConfigField[]
  onRevert: () => void
}) {
  const recommended = toFormValuesFromRecommendation(recommendation.recommendation)
  const currency = recommendation.currency
  const metrics = recommendation.metricsUsed

  return (
    <div className="flex flex-col gap-4 rounded-lg border border-primary/30 bg-primary/5 p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex items-center gap-2">
          <Sparkles className="size-4 text-primary" />
          <h4 className="text-sm font-semibold">Recomandare AI</h4>
          <Badge variant={CONFIDENCE_VARIANTS[recommendation.confidence]}>
            Încredere: {CONFIDENCE_LABELS[recommendation.confidence]}
          </Badge>
        </div>
        <span className="text-xs text-muted-foreground">
          Generată la {formatTimestamp(recommendation.generatedAt)}
        </span>
      </div>

      {recommendation.summary && <p className="text-sm">{recommendation.summary}</p>}

      <p className="text-xs text-muted-foreground">
        Valorile au fost completate în formular, dar <strong>nu au fost salvate</strong>. Apasă „Salvează
        configurația” pentru a le aplica.
      </p>

      {recommendation.reasons.length > 0 && (
        <div className="flex flex-col gap-1">
          <h5 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Motive</h5>
          <ul className="list-disc pl-5 text-sm">
            {recommendation.reasons.map((reason) => (
              <li key={reason}>{reason}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="flex flex-col gap-1">
        <h5 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Indicatori folosiți
        </h5>
        <dl className="grid gap-x-4 gap-y-1 text-sm sm:grid-cols-2">
          <div className="flex justify-between gap-2 sm:justify-start">
            <dt className="text-muted-foreground">Fereastră analizată:</dt>
            <dd>{metrics.windowDays} zile</dd>
          </div>
          <div className="flex justify-between gap-2 sm:justify-start">
            <dt className="text-muted-foreground">Nopți rezervate:</dt>
            <dd>
              {metrics.bookedNights} / {metrics.windowNights} ({formatPercent(metrics.occupancyRate)})
            </dd>
          </div>
          <div className="flex justify-between gap-2 sm:justify-start">
            <dt className="text-muted-foreground">Tarif mediu realizat:</dt>
            <dd>{formatMoney(metrics.averageDailyRate, currency)}</dd>
          </div>
          <div className="flex justify-between gap-2 sm:justify-start">
            <dt className="text-muted-foreground">Tarif de bază:</dt>
            <dd>{formatMoney(metrics.basePricePerNight, currency)}</dd>
          </div>
          <div className="flex justify-between gap-2 sm:justify-start">
            <dt className="text-muted-foreground">Perioade sezoniere:</dt>
            <dd>{metrics.seasonalRatesConfigured}</dd>
          </div>
          <div className="flex justify-between gap-2 sm:justify-start">
            <dt className="text-muted-foreground">Evenimente locale viitoare:</dt>
            <dd>{metrics.upcomingLocalEvents}</dd>
          </div>
        </dl>
      </div>

      {recommendation.missingData.length > 0 && (
        <div className="flex flex-col gap-1">
          <h5 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Date lipsă</h5>
          <ul className="list-disc pl-5 text-sm text-muted-foreground">
            {recommendation.missingData.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      )}

      {recommendation.warnings.length > 0 && (
        <Alert variant="destructive">
          <AlertTitle>Avertismente</AlertTitle>
          <AlertDescription>
            <ul className="list-disc pl-5">
              {recommendation.warnings.map((warning) => (
                <li key={warning}>{warning}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      )}

      <div className="flex flex-col gap-1">
        <h5 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Valori anterioare vs. recomandate
        </h5>
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Setare</TableHead>
                <TableHead>Valoare anterioară</TableHead>
                <TableHead>Recomandat</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {CONFIG_FIELDS.map((field) => {
                const changed = changedFields.includes(field)
                return (
                  <TableRow key={field} data-changed={changed || undefined}>
                    <TableCell>{CONFIG_FIELD_LABELS[field]}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatConfigValue(field, previousValues, currency)}
                    </TableCell>
                    <TableCell className={changed ? "font-semibold text-primary" : undefined}>
                      {formatConfigValue(field, recommended, currency)}
                      {changed && <span className="sr-only"> (modificat)</span>}
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </div>
        {changedFields.length === 0 && (
          <p className="text-sm text-muted-foreground">
            Recomandarea coincide cu valorile actuale — nu e nimic de schimbat.
          </p>
        )}
      </div>

      <Button type="button" variant="outline" size="sm" className="self-start" onClick={onRevert}>
        <RotateCcw className="size-4" />
        Revino la valorile anterioare
      </Button>
    </div>
  )
}

function PricingConfigForm({ propertyId }: { propertyId: string }) {
  const { data: config, isLoading } = usePricingConfig(propertyId)
  const updateConfig = useUpdatePricingConfig(propertyId)
  const aiRecommendation = useAiPricingRecommendation(propertyId)
  const { data: user } = useCurrentUser()

  // Same roles the endpoint allows, and the same ones that gate this whole
  // section on the property page.
  const canUseAi = user?.role === "SUPER_ADMIN" || user?.role === "ADMINISTRATOR"

  const [recommendation, setRecommendation] = useState<AiPricingRecommendationResponse | null>(null)
  /** Form values as they stood immediately before a recommendation was applied. */
  const [previousValues, setPreviousValues] = useState<ConfigFormValues | null>(null)
  const [changedFields, setChangedFields] = useState<ConfigField[]>([])
  /** Bumped on each applied recommendation so the highlight window restarts. */
  const [highlightToken, setHighlightToken] = useState(0)
  const [highlightActive, setHighlightActive] = useState(false)

  const form = useForm<ConfigFormValues>({
    resolver: zodResolver(dynamicPricingConfigSchema),
    defaultValues: DEFAULT_CONFIG_VALUES,
  })

  useEffect(() => {
    if (config) form.reset(toFormValues(config))
  }, [config, form])

  // Switching property: the previous property's recommendation must not linger
  // next to another property's numbers, and its "revert" target is meaningless.
  const resetRecommendation = aiRecommendation.reset
  useEffect(() => {
    setRecommendation(null)
    setPreviousValues(null)
    setChangedFields([])
    setHighlightActive(false)
    resetRecommendation()
  }, [propertyId, resetRecommendation])

  useEffect(() => {
    if (highlightToken === 0) return
    setHighlightActive(true)
    const timer = window.setTimeout(() => setHighlightActive(false), AI_HIGHLIGHT_MS)
    return () => window.clearTimeout(timer)
  }, [highlightToken])

  function requestRecommendation() {
    // One request at a time: a second click while the first is in flight would
    // race two answers into the same form.
    if (aiRecommendation.isPending) return
    const requestedFor = propertyId

    aiRecommendation.mutate(undefined, {
      onSuccess: (response) => {
        // Belt and braces alongside the hook's own check: if the admin moved on
        // to another property while this was in flight, drop the answer.
        if (response.propertyId !== requestedFor || requestedFor !== propertyId) return

        const current = form.getValues()
        const next = toFormValuesFromRecommendation(response.recommendation)

        setPreviousValues(current)
        setRecommendation(response)
        setChangedFields(CONFIG_FIELDS.filter((field) => current[field] !== next[field]))
        setHighlightToken((token) => token + 1)

        // keepDefaultValues leaves defaultValues as the last saved config, so
        // formState.isDirty keeps reporting honestly that there is something
        // unsaved. Nothing is persisted here.
        form.reset(next, { keepDefaultValues: true })
      },
    })
  }

  function revertRecommendation() {
    if (!previousValues) return
    form.reset(previousValues, { keepDefaultValues: true })
    setRecommendation(null)
    setPreviousValues(null)
    setChangedFields([])
    setHighlightActive(false)
    aiRecommendation.reset()
  }

  /** Temporary ring on the fields a recommendation actually changed. */
  function highlightClass(field: ConfigField) {
    return highlightActive && changedFields.includes(field)
      ? "rounded-md ring-2 ring-primary/70 ring-offset-4 ring-offset-background transition-shadow"
      : undefined
  }

  function onSubmit(values: ConfigFormValues) {
    updateConfig.mutate(values, {
      onSuccess: () => {
        // Saved values are the new baseline, so the comparison and the revert
        // target no longer describe anything that exists.
        setRecommendation(null)
        setPreviousValues(null)
        setChangedFields([])
        setHighlightActive(false)
      },
    })
  }

  const header = (
    <div className="flex flex-wrap items-center justify-between gap-2">
      <h3 className="text-sm font-semibold text-muted-foreground">Configurare</h3>
      {canUseAi && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={requestRecommendation}
          disabled={isLoading || aiRecommendation.isPending}
        >
          <Sparkles className="size-4" />
          {aiRecommendation.isPending ? "Se generează..." : "Recomandare AI"}
        </Button>
      )}
    </div>
  )

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3">
        {header}
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      {header}

      {aiRecommendation.isPending && (
        <p className="text-sm text-muted-foreground" role="status">
          Se analizează ocuparea și tarifele proprietății...
        </p>
      )}

      {aiRecommendation.isError && !aiRecommendation.isPending && (
        <Alert variant="destructive">
          <AlertTitle>Recomandarea AI a eșuat</AlertTitle>
          <AlertDescription>
            {aiRecommendationErrorMessage(aiRecommendation.error)} Configurația de mai jos a rămas neschimbată.
          </AlertDescription>
        </Alert>
      )}

      {recommendation && previousValues && (
        <AiRecommendationCard
          recommendation={recommendation}
          previousValues={previousValues}
          changedFields={changedFields}
          onRevert={revertRecommendation}
        />
      )}

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <FormField
            control={form.control}
            name="enabled"
            render={({ field }) => (
              <FormItem className={highlightClass("enabled")}>
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
                <FormItem className={highlightClass("minPrice")}>
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
                <FormItem className={highlightClass("maxPrice")}>
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
                <FormItem className={highlightClass("occupancyWindowDays")}>
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
                <FormItem className={highlightClass("occupancyMultiplierMin")}>
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
                <FormItem className={highlightClass("occupancyMultiplierMax")}>
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
                <FormItem className={highlightClass("leadTimeDays")}>
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
                <FormItem className={highlightClass("leadTimeMultiplier")}>
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

          <div className="flex flex-wrap items-center gap-3">
            <Button type="submit" disabled={updateConfig.isPending}>
              {updateConfig.isPending ? "Se salvează..." : "Salvează configurația"}
            </Button>
            {form.formState.isDirty && (
              <span className="text-sm text-amber-600 dark:text-amber-500" role="status">
                Ai modificări nesalvate
              </span>
            )}
          </div>
        </form>
      </Form>
    </div>
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
      {/* The "Configurare" heading lives inside the form, so the AI button can
          sit beside it on the same row. */}
      <PricingConfigForm propertyId={propertyId} />

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
