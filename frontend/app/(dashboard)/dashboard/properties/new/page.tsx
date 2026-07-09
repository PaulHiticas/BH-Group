"use client"

import { PropertyForm, propertyFormValuesToPayload } from "@/components/properties/property-form"
import { useCreateProperty } from "@/hooks/use-properties"

export default function NewPropertyPage() {
  const createProperty = useCreateProperty()

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Adaugă proprietate</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Proprietatea va fi creată cu statusul &quot;Ciornă&quot; și poate fi activată ulterior.
        </p>
      </div>

      <PropertyForm
        mode="create"
        isSubmitting={createProperty.isPending}
        onSubmit={(values) => createProperty.mutate(propertyFormValuesToPayload(values))}
      />
    </div>
  )
}
