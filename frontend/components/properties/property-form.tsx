"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import dynamic from "next/dynamic"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import {
  ALL_FACILITIES,
  ALL_PROPERTY_STATUSES,
  ALL_PROPERTY_TYPES,
  FACILITY_LABELS,
  PROPERTY_STATUS_LABELS,
  PROPERTY_TYPE_LABELS,
} from "@/lib/property-labels"
import type { PropertyPayload } from "@/lib/api/properties"
import type { PropertyResponse } from "@/lib/api/types"

const LocationPicker = dynamic(
  () => import("@/components/map/location-picker").then((mod) => mod.LocationPicker),
  { ssr: false, loading: () => <Skeleton className="h-72 w-full" /> }
)

const propertySchema = z.object({
  name: z.string().min(1, "Numele este obligatoriu").max(200),
  description: z.string().max(4000).optional(),
  propertyType: z.enum(ALL_PROPERTY_TYPES as [string, ...string[]]),
  status: z.enum(ALL_PROPERTY_STATUSES as [string, ...string[]]),
  addressLine: z.string().min(1, "Adresa este obligatorie").max(255),
  city: z.string().min(1, "Orașul este obligatoriu").max(100),
  county: z.string().max(100).optional(),
  postalCode: z.string().max(20).optional(),
  country: z.string().min(1, "Țara este obligatorie").max(100),
  latitude: z.coerce.number().min(-90).max(90).optional(),
  longitude: z.coerce.number().min(-180).max(180).optional(),
  bedrooms: z.coerce.number().int().min(0),
  bathrooms: z.coerce.number().int().min(0),
  maxGuests: z.coerce.number().int().min(1),
  sizeSqm: z.coerce.number().min(0).optional(),
  basePricePerNight: z.coerce.number().min(0).optional(),
  checkInTime: z.string().min(1),
  checkOutTime: z.string().min(1),
  facilities: z.array(z.enum(ALL_FACILITIES as [string, ...string[]])),
  smartLockEnabled: z.boolean(),
  smartLockProvider: z.string().max(100).optional(),
  smartLockDeviceId: z.string().max(150).optional(),
})

export type PropertyFormValues = z.infer<typeof propertySchema>

function toFormValues(property?: PropertyResponse): PropertyFormValues {
  if (!property) {
    return {
      name: "",
      description: "",
      propertyType: "APARTMENT",
      status: "DRAFT",
      addressLine: "",
      city: "",
      county: "",
      postalCode: "",
      country: "Romania",
      latitude: undefined,
      longitude: undefined,
      bedrooms: 1,
      bathrooms: 1,
      maxGuests: 2,
      sizeSqm: undefined,
      basePricePerNight: undefined,
      checkInTime: "14:00",
      checkOutTime: "11:00",
      facilities: [],
      smartLockEnabled: false,
      smartLockProvider: "",
      smartLockDeviceId: "",
    }
  }

  return {
    name: property.name,
    description: property.description ?? "",
    propertyType: property.propertyType,
    status: property.status,
    addressLine: property.address.addressLine,
    city: property.address.city,
    county: property.address.county ?? "",
    postalCode: property.address.postalCode ?? "",
    country: property.address.country,
    latitude: property.address.latitude ?? undefined,
    longitude: property.address.longitude ?? undefined,
    bedrooms: property.bedrooms,
    bathrooms: property.bathrooms,
    maxGuests: property.maxGuests,
    sizeSqm: property.sizeSqm ?? undefined,
    basePricePerNight: property.basePricePerNight ?? undefined,
    checkInTime: property.checkInTime.slice(0, 5),
    checkOutTime: property.checkOutTime.slice(0, 5),
    facilities: property.facilities,
    smartLockEnabled: property.smartLockEnabled,
    smartLockProvider: property.smartLockProvider ?? "",
    smartLockDeviceId: property.smartLockDeviceId ?? "",
  }
}

export function propertyFormValuesToPayload(
  values: PropertyFormValues
): PropertyPayload & { status: PropertyFormValues["status"] } {
  return {
    name: values.name,
    description: values.description || undefined,
    propertyType: values.propertyType as PropertyPayload["propertyType"],
    status: values.status as never,
    address: {
      addressLine: values.addressLine,
      city: values.city,
      county: values.county || undefined,
      postalCode: values.postalCode || undefined,
      country: values.country,
      latitude: values.latitude ?? null,
      longitude: values.longitude ?? null,
    },
    bedrooms: values.bedrooms,
    bathrooms: values.bathrooms,
    maxGuests: values.maxGuests,
    sizeSqm: values.sizeSqm ?? null,
    basePricePerNight: values.basePricePerNight ?? null,
    checkInTime: `${values.checkInTime}:00`,
    checkOutTime: `${values.checkOutTime}:00`,
    facilities: values.facilities as PropertyPayload["facilities"],
    smartLockEnabled: values.smartLockEnabled,
    smartLockProvider: values.smartLockProvider || undefined,
    smartLockDeviceId: values.smartLockDeviceId || undefined,
  }
}

interface PropertyFormProps {
  property?: PropertyResponse
  mode: "create" | "edit"
  onSubmit: (values: PropertyFormValues) => void
  isSubmitting?: boolean
}

export function PropertyForm({ property, mode, onSubmit, isSubmitting }: PropertyFormProps) {
  const form = useForm({
    resolver: zodResolver(propertySchema),
    defaultValues: toFormValues(property),
  })

  const smartLockEnabled = form.watch("smartLockEnabled")

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Informații generale</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <FormLabel>Nume proprietate</FormLabel>
                  <FormControl>
                    <Input placeholder="Apartament Central Studio" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <FormLabel>Descriere</FormLabel>
                  <FormControl>
                    <Textarea rows={4} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="propertyType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tip proprietate</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {ALL_PROPERTY_TYPES.map((type) => (
                        <SelectItem key={type} value={type}>
                          {PROPERTY_TYPE_LABELS[type]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            {mode === "edit" && (
              <FormField
                control={form.control}
                name="status"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Status</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {ALL_PROPERTY_STATUSES.map((status) => (
                          <SelectItem key={status} value={status}>
                            {PROPERTY_STATUS_LABELS[status]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Adresă</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="addressLine"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <FormLabel>Adresă</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="city"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Oraș</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="county"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Județ</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="postalCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cod poștal</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="country"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Țară</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="sm:col-span-2">
              <FormLabel className="mb-2 block">Locație pe hartă</FormLabel>
              <LocationPicker
                latitude={form.watch("latitude") as number | undefined}
                longitude={form.watch("longitude") as number | undefined}
                searchHint={[form.watch("addressLine"), form.watch("city"), form.watch("country")]
                  .filter(Boolean)
                  .join(", ")}
                onChange={(lat, lng) => {
                  form.setValue("latitude", lat, { shouldDirty: true })
                  form.setValue("longitude", lng, { shouldDirty: true })
                }}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Detalii & Check-in</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-3">
            <FormField
              control={form.control}
              name="bedrooms"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Dormitoare</FormLabel>
                  <FormControl>
                    <Input type="number" min={0} {...field} value={field.value as number} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="bathrooms"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Băi</FormLabel>
                  <FormControl>
                    <Input type="number" min={0} {...field} value={field.value as number} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="maxGuests"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Max. oaspeți</FormLabel>
                  <FormControl>
                    <Input type="number" min={1} {...field} value={field.value as number} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="sizeSqm"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Suprafață (mp)</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="0.1"
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
              name="basePricePerNight"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Preț / noapte (RON)</FormLabel>
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
              name="checkInTime"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Check-in</FormLabel>
                  <FormControl>
                    <Input type="time" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="checkOutTime"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Check-out</FormLabel>
                  <FormControl>
                    <Input type="time" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Facilități</CardTitle>
          </CardHeader>
          <CardContent>
            <FormField
              control={form.control}
              name="facilities"
              render={({ field }) => (
                <FormItem>
                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
                    {ALL_FACILITIES.map((facility) => {
                      const checked = field.value.includes(facility)
                      return (
                        <label
                          key={facility}
                          className="flex items-center gap-2 text-sm"
                        >
                          <Checkbox
                            checked={checked}
                            onCheckedChange={(value) => {
                              if (value) {
                                field.onChange([...field.value, facility])
                              } else {
                                field.onChange(field.value.filter((f) => f !== facility))
                              }
                            }}
                          />
                          {FACILITY_LABELS[facility]}
                        </label>
                      )
                    })}
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Smart Lock</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="smartLockEnabled"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <label className="flex items-center gap-2 text-sm">
                    <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                    Această proprietate are Smart Lock
                  </label>
                </FormItem>
              )}
            />
            {smartLockEnabled && (
              <>
                <FormField
                  control={form.control}
                  name="smartLockProvider"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Furnizor</FormLabel>
                      <FormControl>
                        <Input placeholder="Nuki, TTLock, ..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="smartLockDeviceId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>ID Dispozitiv</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </>
            )}
          </CardContent>
        </Card>

        <div className="flex justify-end">
          <Button type="submit" size="lg" disabled={isSubmitting}>
            {isSubmitting
              ? "Se salvează..."
              : mode === "create"
                ? "Creează proprietate"
                : "Salvează modificările"}
          </Button>
        </div>
      </form>
    </Form>
  )
}
