"use client"

import { Suspense, useState } from "react"
import { useSearchParams } from "next/navigation"
import { Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { PublicPropertyCard } from "@/components/booking/public-property-card"
import { usePublicProperties } from "@/hooks/use-public-booking"

function SearchForm() {
  const initialParams = useSearchParams()
  const [search, setSearch] = useState(initialParams.get("search") ?? "")
  const [guests, setGuests] = useState<number | undefined>(
    initialParams.get("guests") ? Number(initialParams.get("guests")) : undefined
  )
  const [checkIn, setCheckIn] = useState(initialParams.get("checkIn") ?? "")
  const [checkOut, setCheckOut] = useState(initialParams.get("checkOut") ?? "")
  const [page, setPage] = useState(0)

  const { data, isLoading } = usePublicProperties({
    search: search || undefined,
    guests,
    checkIn: checkIn || undefined,
    checkOut: checkOut || undefined,
    page,
  })

  const searchParams = new URLSearchParams()
  if (checkIn) searchParams.set("checkIn", checkIn)
  if (checkOut) searchParams.set("checkOut", checkOut)
  if (guests) searchParams.set("guests", String(guests))

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-8">
      <div className="text-center">
        <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
          Găsește-ți următoarea vacanță
        </h1>
        <p className="mt-2 text-muted-foreground">
          Caută dintre proprietățile noastre disponibile pentru închiriere.
        </p>
      </div>

      <div className="mx-auto flex w-full max-w-3xl flex-wrap items-end gap-3 rounded-2xl border border-border/60 bg-card p-4 shadow-sm">
        <div className="flex min-w-48 flex-1 flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Destinație</label>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Oraș sau nume proprietate"
              className="pl-9"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
                setPage(0)
              }}
            />
          </div>
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Check-in</label>
          <Input
            type="date"
            value={checkIn}
            onChange={(e) => {
              setCheckIn(e.target.value)
              setPage(0)
            }}
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Check-out</label>
          <Input
            type="date"
            value={checkOut}
            onChange={(e) => {
              setCheckOut(e.target.value)
              setPage(0)
            }}
          />
        </div>
        <div className="flex w-24 flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Oaspeți</label>
          <Input
            type="number"
            min={1}
            value={guests ?? ""}
            onChange={(e) => {
              setGuests(e.target.value ? Number(e.target.value) : undefined)
              setPage(0)
            }}
          />
        </div>
        <Button type="button" onClick={() => setPage(0)}>
          Caută
        </Button>
      </div>

      {isLoading ? (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[4/3] w-full rounded-2xl" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border/60 py-16 text-center">
          <p className="text-sm text-muted-foreground">
            Nu au fost găsite proprietăți disponibile pentru criteriile selectate.
          </p>
        </div>
      ) : (
        <>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {data.content.map((property) => (
              <PublicPropertyCard
                key={property.id}
                property={property}
                searchParams={searchParams.toString()}
              />
            ))}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}

export function BookingSearchView() {
  return (
    <Suspense fallback={null}>
      <SearchForm />
    </Suspense>
  )
}
