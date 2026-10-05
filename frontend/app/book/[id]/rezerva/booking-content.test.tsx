import { beforeEach, describe, expect, it, vi } from "vitest"
import { renderWithProviders, screen, userEvent, waitFor } from "@/test/utils"
import { mockSearchParams } from "@/test/mocks/next-navigation"
import { publicReservation } from "@/test/fixtures/public-reservation"
import type { PublicReservationResponse } from "@/lib/api/types"
import { BookingContent } from "./booking-content"

const { getProperty, getPaymentConfig, startCardCheckout, redirectToExternal } = vi.hoisted(() => ({
  getProperty: vi.fn(),
  getPaymentConfig: vi.fn(),
  startCardCheckout: vi.fn(),
  redirectToExternal: vi.fn(),
}))
vi.mock("@/lib/api/public", () => ({ publicApi: { getProperty, getPaymentConfig, startCardCheckout } }))
vi.mock("@/lib/redirect", () => ({ redirectToExternal }))

// The real form (fields, availability, quote) is covered by its own tests;
// here it only matters what happens once it has created the held booking.
vi.mock("@/components/booking/booking-form", () => ({
  BookingForm: (props: {
    onSuccess: (reservation: PublicReservationResponse) => void
    submitLabel?: string
    busy?: boolean
  }) => (
    <button type="button" disabled={props.busy} onClick={() => props.onSuccess(publicReservation())}>
      {props.submitLabel}
    </button>
  ),
}))

describe("BookingContent", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSearchParams.value = new URLSearchParams()
    getProperty.mockResolvedValue({ id: "prop-1", name: "Apartament Test" })
  })

  it("sends the guest straight to Stripe Checkout once the booking is held, with no manual approval step", async () => {
    getPaymentConfig.mockResolvedValue({ cardPaymentsEnabled: true, publishableKey: "pk_test" })
    startCardCheckout.mockResolvedValue({
      checkoutUrl: "https://checkout.stripe.com/c/pay/cs_1",
      amount: 500,
      currency: "RON",
    })
    const user = userEvent.setup()

    renderWithProviders(<BookingContent id="prop-1" />)
    await user.click(await screen.findByRole("button", { name: "Continuă către plată" }))

    await waitFor(() => expect(redirectToExternal).toHaveBeenCalledWith("https://checkout.stripe.com/c/pay/cs_1"))
    expect(startCardCheckout).toHaveBeenCalledTimes(1)
    expect(startCardCheckout).toHaveBeenCalledWith("tok-123")
    expect(
      screen.getByRole("heading", { name: "Te redirecționăm către plata securizată" })
    ).toBeInTheDocument()
    expect(screen.queryByText(/așteaptă confirmarea/)).not.toBeInTheDocument()
  })

  it("offers a retry when checkout cannot be opened, and ignores repeated clicks on it", async () => {
    getPaymentConfig.mockResolvedValue({ cardPaymentsEnabled: true, publishableKey: "pk_test" })
    startCardCheckout.mockRejectedValueOnce(new Error("network")).mockResolvedValue({
      checkoutUrl: "https://checkout.stripe.com/c/pay/cs_2",
      amount: 500,
      currency: "RON",
    })
    const user = userEvent.setup()

    renderWithProviders(<BookingContent id="prop-1" />)
    await user.click(await screen.findByRole("button", { name: "Continuă către plată" }))

    const retry = await screen.findByRole("button", { name: "Reîncearcă plata cu cardul" })
    await user.click(retry)
    await user.click(retry)

    await waitFor(() => expect(redirectToExternal).toHaveBeenCalledWith("https://checkout.stripe.com/c/pay/cs_2"))
    // one automatic attempt + one retry, not one per click
    expect(startCardCheckout).toHaveBeenCalledTimes(2)
    expect(
      screen.getByRole("heading", { name: "Te redirecționăm către plata securizată" })
    ).toBeInTheDocument()
  })

  it("explains clearly when card payment is unavailable and never opens checkout", async () => {
    getPaymentConfig.mockResolvedValue({ cardPaymentsEnabled: false, publishableKey: null })
    const user = userEvent.setup()

    renderWithProviders(<BookingContent id="prop-1" />)

    expect(await screen.findByText(/Plata online cu cardul nu este disponibilă momentan/)).toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: "Trimite cererea de rezervare" }))

    expect(await screen.findByRole("heading", { name: /așteaptă confirmarea/ })).toBeInTheDocument()
    expect(startCardCheckout).not.toHaveBeenCalled()
    expect(redirectToExternal).not.toHaveBeenCalled()
  })
})
