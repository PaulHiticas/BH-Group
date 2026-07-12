"use client"

import { useState } from "react"
import { Plus, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  useAddSeasonalRate,
  useDeleteSeasonalRate,
  useSeasonalRates,
} from "@/hooks/use-properties"

export function SeasonalRatesManager({
  propertyId,
  canManage,
}: {
  propertyId: string
  canManage: boolean
}) {
  const { data: rates, isLoading } = useSeasonalRates(propertyId)
  const addRate = useAddSeasonalRate(propertyId)
  const deleteRate = useDeleteSeasonalRate(propertyId)

  const [label, setLabel] = useState("")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [pricePerNight, setPricePerNight] = useState("")

  function handleAdd() {
    if (!label || !startDate || !endDate || !pricePerNight) return
    addRate.mutate(
      { label, startDate, endDate, pricePerNight: Number(pricePerNight) },
      {
        onSuccess: () => {
          setLabel("")
          setStartDate("")
          setEndDate("")
          setPricePerNight("")
        },
      }
    )
  }

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Se încarcă sezoanele...</p>
  }

  return (
    <div className="flex flex-col gap-3">
      {!rates || rates.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Niciun sezon definit — se aplică prețul standard (și cel de weekend, dacă e setat).
        </p>
      ) : (
        <div className="flex flex-col divide-y divide-border/60">
          {rates.map((rate) => (
            <div key={rate.id} className="flex items-center justify-between py-2.5 text-sm">
              <div>
                <span className="font-medium">{rate.label}</span>
                <span className="ml-2 text-muted-foreground">
                  {rate.startDate} → {rate.endDate}
                </span>
              </div>
              <div className="flex items-center gap-3">
                <span>{rate.pricePerNight} RON / noapte</span>
                {canManage && (
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    onClick={() => deleteRate.mutate(rate.id)}
                    aria-label="Șterge sezon"
                  >
                    <Trash2 className="size-4 text-destructive" />
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {canManage && (
        <div className="grid gap-2 pt-2 sm:grid-cols-5">
          <Input
            placeholder="Etichetă (ex: Vară 2026)"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            className="sm:col-span-2"
          />
          <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          <div className="flex gap-2">
            <Input
              type="number"
              min={0}
              step="0.01"
              placeholder="Preț/noapte"
              value={pricePerNight}
              onChange={(e) => setPricePerNight(e.target.value)}
            />
            <Button
              size="icon"
              variant="outline"
              disabled={addRate.isPending}
              onClick={handleAdd}
              aria-label="Adaugă sezon"
            >
              <Plus className="size-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
