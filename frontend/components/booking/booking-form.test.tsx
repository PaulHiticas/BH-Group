import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { renderWithProviders, screen, userEvent, waitFor, within } from "@/test/utils"
import type { PublicPropertyResponse } from "@/lib/api/types"
import { BookingForm } from "./booking-form"

const mutate = vi.fn()
const usePublicAvailability = vi.fn<(...args: unknown[]) => { data: undefined; isFetching: boolean }>(
  () => ({ data: undefined, isFetching: false })
)
const usePublicQuote = vi.fn<(...args: unknown[]) => { data: undefined }>(() => ({ data: undefined }))

vi.mock("@/hooks/use-public-booking", () => ({
  useCreatePublicBooking: () => ({ mutate, isPending: false }),
  usePublicAvailability: (...args: unknown[]) => usePublicAvailability(...args),
  usePublicQuote: (...args: unknown[]) => usePublicQuote(...args),
}))

const property: PublicPropertyResponse = {
  id: "prop-1",
  name: "Apartament Test",
  description: null,
  propertyType: "APARTMENT",
  addressLine: null,
  city: "Cluj-Napoca",
  county: null,
  country: "România",
  latitude: null,
  longitude: null,
  exactLocation: false,
  bedrooms: 2,
  bathrooms: 1,
  maxGuests: 4,
  minStayNights: null,
  maxStayNights: null,
  sizeSqm: null,
  basePricePerNight: 300,
  currency: "RON",
  checkInTime: "14:00:00",
  checkOutTime: "11:00:00",
  facilities: [],
  photos: [],
}

function renderForm(props: Partial<React.ComponentProps<typeof BookingForm>> = {}) {
  return renderWithProviders(<BookingForm property={property} onSuccess={vi.fn()} {...props} />)
}

describe("BookingForm dates", () => {
  beforeEach(() => {
    // Only Date is faked, so user-event's timers keep working.
    vi.useFakeTimers({ toFake: ["Date"] })
    vi.setSystemTime(new Date(2026, 8, 1))
    mutate.mockClear()
    usePublicAvailability.mockClear()
    usePublicQuote.mockClear()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it("uses the range calendar instead of native date inputs", () => {
    const { container } = renderForm()
    expect(container.querySelector('input[type="date"]')).toBeNull()
    expect(screen.getByLabelText("Perioadă")).toHaveTextContent("Selectează datele")
  })

  it("feeds the dates picked in the calendar into the quote and the booking payload", async () => {
    const user = userEvent.setup()
    renderForm()

    await user.click(screen.getByLabelText("Perioadă"))
    const grid = await screen.findByRole("grid")
    await user.click(within(grid).getByText("10"))
    await user.click(within(grid).getByText("14"))

    await waitFor(() =>
      expect(usePublicQuote).toHaveBeenLastCalledWith("prop-1", "2026-09-10", "2026-09-14", 2)
    )
    expect(usePublicAvailability).toHaveBeenLastCalledWith("prop-1", "2026-09-10", "2026-09-14")

    await user.type(screen.getByLabelText("Prenume"), "Ana")
    await user.type(screen.getByLabelText("Nume"), "Pop")
    await user.type(screen.getByLabelText("Email"), "ana@example.com")
    await user.type(screen.getByLabelText("Telefon"), "0700000000")
    await user.click(screen.getByRole("button", { name: "Trimite cererea de rezervare" }))

    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1))
    expect(mutate.mock.calls[0][0]).toMatchObject({
      propertyId: "prop-1",
      checkInDate: "2026-09-10",
      checkOutDate: "2026-09-14",
      numberOfGuests: 2,
    })
  })

  it("shows the required-date error on submit and clears it once dates are picked", async () => {
    const user = userEvent.setup()
    renderForm()

    await user.click(screen.getByRole("button", { name: "Trimite cererea de rezervare" }))
    expect(await screen.findByText("Data de check-in este obligatorie")).toBeInTheDocument()

    await user.click(screen.getByLabelText("Perioadă"))
    const grid = await screen.findByRole("grid")
    await user.click(within(grid).getByText("10"))
    await user.click(within(grid).getByText("14"))

    await waitFor(() =>
      expect(screen.queryByText("Data de check-in este obligatorie")).not.toBeInTheDocument()
    )
    expect(screen.queryByText("Data de check-out este obligatorie")).not.toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()
  })

  it("keeps a check-in carried over from the URL in the calendar trigger", () => {
    renderForm({ defaultCheckIn: "2026-09-10" })
    expect(screen.getByLabelText("Perioadă")).toHaveTextContent("10 sep – ...")
  })

  it("keeps the locked summary when both dates come from the URL", () => {
    renderForm({ defaultCheckIn: "2026-09-10", defaultCheckOut: "2026-09-14" })
    expect(screen.queryByLabelText("Perioadă")).toBeNull()
    expect(screen.getByText("2026-09-10 → 2026-09-14")).toBeInTheDocument()
    expect(screen.getByText("Modifică perioada")).toBeInTheDocument()
  })
})
