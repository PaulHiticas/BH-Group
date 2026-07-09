"use client"

import { use } from "react"
import { Skeleton } from "@/components/ui/skeleton"
import { PropertyForm, propertyFormValuesToPayload } from "@/components/properties/property-form"
import { useProperty, useUpdateProperty } from "@/hooks/use-properties"

export default function EditPropertyPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: property, isLoading } = useProperty(id)
  const updateProperty = useUpdateProperty(id)

  if (isLoading || !property) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Editează proprietate</h1>
        <p className="mt-1 text-sm text-muted-foreground">{property.name}</p>
      </div>

      <PropertyForm
        mode="edit"
        property={property}
        isSubmitting={updateProperty.isPending}
        onSubmit={(values) => updateProperty.mutate(propertyFormValuesToPayload(values))}
      />
    </div>
  )
}
