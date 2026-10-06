import { describe, expect, it } from "vitest"
import { renderWithProviders, screen, within } from "@/test/utils"
import { OwnerStatementBreakdown } from "./owner-statement-breakdown"
import { OwnerRevenueLines } from "./owner-revenue-lines"
import type { OwnerRevenueLine, OwnerStatementResponse } from "@/lib/api/types"

function statement(overrides: Partial<OwnerStatementResponse>): OwnerStatementResponse {
  return {
    id: "s1",
    ownerId: "o1",
    ownerName: "Maria Ionescu",
    periodStart: "2026-07-01",
    periodEnd: "2026-07-31",
    currency: "RON",
    grossRevenue: 1000,
    commissionAmount: 160,
    expensesTotal: 100,
    netPayout: 740,
    status: "ISSUED",
    generatedByName: "Admin",
    paidAt: null,
    paymentReference: null,
    createdAt: "2026-08-01T10:00:00Z",
    lines: [
      {
        propertyId: "p1",
        propertyName: "Casa Mare",
        grossRevenue: 1000,
        commissionAmount: 160,
        expensesTotal: 100,
        netAmount: 740,
        capturedTotal: 1100,
        refundedTotal: 100,
        commissionableBase: 800,
        commissionPercent: 20,
        ownerAmount: 840,
        unallocatedNetRevenue: 0,
        unallocatedReservationCount: 0,
      },
    ],
    calculationMethod: "CAPTURED_ACCOMMODATION",
    capturedTotal: 1100,
    refundedTotal: 100,
    commissionableBase: 800,
    ownerAmount: 840,
    unallocatedNetRevenue: 250,
    unallocatedReservationCount: 1,
    ...overrides,
  }
}

/** A value from the statement summary (labels also appear as table headers). */
function figure(label: string) {
  return within(screen.getByLabelText("Rezumat decont RON")).getByText(label).nextSibling
}

describe("OwnerStatementBreakdown", () => {
  it("shows collected money, refunds, base, commission, owner amount and payout", () => {
    renderWithProviders(<OwnerStatementBreakdown statement={statement({})} />)

    expect(figure("Încasat")).toHaveTextContent(/1\.100,00\sRON/)
    expect(figure("Refunduri")).toHaveTextContent(/-100,00\sRON/)
    expect(figure("Venit net încasat")).toHaveTextContent(/1\.000,00\sRON/)
    expect(figure("Bază comisionabilă (cazare)")).toHaveTextContent(/800,00\sRON/)
    expect(figure("Comision BH Stays")).toHaveTextContent(/-160,00\sRON/)
    expect(figure("Sumă proprietar")).toHaveTextContent(/840,00\sRON/)
    expect(figure("Net de plată")).toHaveTextContent(/740,00\sRON/)
    expect(screen.getByRole("status")).toHaveTextContent(/1 rezervare fără defalcare a prețului \(250,00\sRON\)/)

    const line = screen.getByRole("row", { name: /Casa Mare/ })
    expect(line).toHaveTextContent("20%")
    expect(line).toHaveTextContent(/840,00\sRON/)
  })

  it("marks statements issued with the old method and shows them as issued", () => {
    renderWithProviders(
      <OwnerStatementBreakdown
        statement={statement({
          calculationMethod: "LEGACY_GROSS",
          capturedTotal: null,
          refundedTotal: null,
          commissionableBase: null,
          ownerAmount: null,
          unallocatedNetRevenue: null,
          unallocatedReservationCount: null,
          commissionAmount: 200,
          netPayout: 700,
        })}
      />
    )

    expect(screen.getByRole("note")).toHaveTextContent("metoda veche de calcul")
    expect(figure("Comision BH Stays")).toHaveTextContent(/-200,00\sRON/)
    expect(figure("Net de plată")).toHaveTextContent(/700,00\sRON/)
    expect(screen.queryByText("Bază comisionabilă (cazare)")).not.toBeInTheDocument()
  })
})

function revenueLine(overrides: Partial<OwnerRevenueLine>): OwnerRevenueLine {
  return {
    currency: "RON",
    capturedTotal: 1000,
    refundedTotal: 0,
    netRevenue: 1000,
    commissionableBase: 800,
    commissionPercent: 20,
    bhStaysCommission: 160,
    ownerAmount: 840,
    netPayout: 740,
    expensesTotal: 100,
    unconfiguredNetRevenue: 0,
    unallocatedNetRevenue: 0,
    unallocatedReservationCount: 0,
    ...overrides,
  }
}

describe("OwnerRevenueLines", () => {
  it("shows one block per currency, never a mixed total", () => {
    renderWithProviders(
      <OwnerRevenueLines
        lines={[
          revenueLine({ currency: "EUR", netRevenue: 100, bhStaysCommission: 20, ownerAmount: 80, netPayout: 80, expensesTotal: 0 }),
          revenueLine({}),
        ]}
      />
    )

    const eur = screen.getByRole("region", { name: "Încasări EUR" })
    const ron = screen.getByRole("region", { name: "Încasări RON" })
    expect(within(eur).queryByText(/RON/)).not.toBeInTheDocument()
    expect(within(ron).getByText("Comision BH Stays (20%)").nextSibling).toHaveTextContent(/-160,00\sRON/)
    expect(within(ron).getByText("Net de plată").nextSibling).toHaveTextContent(/740,00\sRON/)
  })

  it("does not invent a split while a commission is not configured", () => {
    renderWithProviders(
      <OwnerRevenueLines
        lines={[revenueLine({ bhStaysCommission: null, ownerAmount: null, netPayout: null, unconfiguredNetRevenue: 500 })]}
      />
    )

    expect(screen.getByText("Sumă proprietar").nextSibling).toHaveTextContent("—")
    expect(screen.getByRole("status")).toHaveTextContent(/500,00\sRON așteaptă configurarea comisionului/)
  })

  it("has an empty state", () => {
    renderWithProviders(<OwnerRevenueLines lines={[]} />)
    expect(screen.getByText("Nicio încasare înregistrată încă.")).toBeInTheDocument()
  })
})
