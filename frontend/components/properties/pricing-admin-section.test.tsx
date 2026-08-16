import { describe, expect, it, vi, beforeEach } from "vitest"
import { fireEvent } from "@testing-library/react"
import { screen, renderWithProviders, userEvent } from "@/test/utils"
import { dynamicPricingConfigSchema, PricingAdminSection } from "./pricing-admin-section"
import type { DynamicPricingConfigResponse } from "@/lib/api/pricing"
import {
  useCreateLocalEvent,
  useDeleteLocalEvent,
  useMergedLocalEvents,
  usePricingBreakdown,
  usePricingConfig,
  useUpdatePricingConfig,
} from "@/hooks/use-pricing"

vi.mock("@/hooks/use-pricing", () => ({
  usePricingConfig: vi.fn(),
  useUpdatePricingConfig: vi.fn(),
  usePricingBreakdown: vi.fn(),
  useMergedLocalEvents: vi.fn(),
  useCreateLocalEvent: vi.fn(),
  useDeleteLocalEvent: vi.fn(),
}))

// ---------------------------------------------------------------------------
// Pure schema tests — the fast, stable way to cover every numeric CHECK rule.
// ---------------------------------------------------------------------------

const validConfig = {
  enabled: false,
  minPrice: null,
  maxPrice: null,
  occupancyWindowDays: 14,
  occupancyMultiplierMin: 0.9,
  occupancyMultiplierMax: 1.3,
  leadTimeDays: 7,
  leadTimeMultiplier: 1,
}

describe("dynamicPricingConfigSchema", () => {
  it("accepts a valid config with null min/max price", () => {
    expect(dynamicPricingConfigSchema.safeParse(validConfig).success).toBe(true)
  })

  it("rejects minPrice greater than maxPrice", () => {
    const result = dynamicPricingConfigSchema.safeParse({ ...validConfig, minPrice: 200, maxPrice: 100 })
    expect(result.success).toBe(false)
  })

  it("accepts minPrice equal to maxPrice", () => {
    const result = dynamicPricingConfigSchema.safeParse({ ...validConfig, minPrice: 100, maxPrice: 100 })
    expect(result.success).toBe(true)
  })

  it("rejects occupancyWindowDays of 0 or below", () => {
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, occupancyWindowDays: 0 }).success).toBe(false)
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, occupancyWindowDays: -1 }).success).toBe(false)
  })

  it("rejects a negative leadTimeDays but accepts zero", () => {
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, leadTimeDays: -1 }).success).toBe(false)
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, leadTimeDays: 0 }).success).toBe(true)
  })

  it("rejects occupancyMultiplierMin of 0 or below", () => {
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, occupancyMultiplierMin: 0 }).success).toBe(false)
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, occupancyMultiplierMin: -0.5 }).success).toBe(false)
  })

  it("rejects occupancyMultiplierMax below occupancyMultiplierMin but accepts equal", () => {
    expect(
      dynamicPricingConfigSchema.safeParse({
        ...validConfig,
        occupancyMultiplierMin: 1,
        occupancyMultiplierMax: 0.9,
      }).success
    ).toBe(false)
    expect(
      dynamicPricingConfigSchema.safeParse({
        ...validConfig,
        occupancyMultiplierMin: 1,
        occupancyMultiplierMax: 1,
      }).success
    ).toBe(true)
  })

  it("rejects leadTimeMultiplier of 0 or below", () => {
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, leadTimeMultiplier: 0 }).success).toBe(false)
    expect(dynamicPricingConfigSchema.safeParse({ ...validConfig, leadTimeMultiplier: -1 }).success).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// Rendered component tests
// ---------------------------------------------------------------------------

const baseConfig: DynamicPricingConfigResponse = {
  propertyId: "prop-1",
  enabled: false,
  minPrice: 100,
  maxPrice: null,
  occupancyWindowDays: 14,
  occupancyMultiplierMin: 0.9,
  occupancyMultiplierMax: 1.3,
  leadTimeDays: 7,
  leadTimeMultiplier: 1,
}

function mockHappyPath(overrides: { updateMutate?: ReturnType<typeof vi.fn> } = {}) {
  vi.mocked(usePricingConfig).mockReturnValue({ data: baseConfig, isLoading: false } as never)
  vi.mocked(useUpdatePricingConfig).mockReturnValue({
    mutate: overrides.updateMutate ?? vi.fn(),
    isPending: false,
  } as never)
  vi.mocked(usePricingBreakdown).mockReturnValue({ data: undefined, isLoading: false, isFetching: false } as never)
  vi.mocked(useMergedLocalEvents).mockReturnValue({ events: [], isLoading: false, isError: false } as never)
  vi.mocked(useCreateLocalEvent).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
  vi.mocked(useDeleteLocalEvent).mockReturnValue({ mutate: vi.fn(), isPending: false } as never)
}

describe("PricingAdminSection", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("shows a real validation error and blocks submission when minPrice > maxPrice", async () => {
    const updateMutate = vi.fn()
    mockHappyPath({ updateMutate })
    const user = userEvent.setup()
    renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

    const maxPriceInput = screen.getByLabelText("Preț maxim (plafon siguranță)")
    await user.type(maxPriceInput, "50")
    await user.click(screen.getByRole("button", { name: "Salvează configurația" }))

    expect(await screen.findByText("Prețul minim nu poate fi mai mare decât maximul")).toBeInTheDocument()
    expect(updateMutate).not.toHaveBeenCalled()
  })

  it("converts an emptied minPrice field to null (not '' or NaN) in the submitted payload", async () => {
    const updateMutate = vi.fn()
    mockHappyPath({ updateMutate })
    const user = userEvent.setup()
    renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

    const minPriceInput = screen.getByLabelText("Preț minim (plafon siguranță)")
    expect(minPriceInput).toHaveValue(100)
    await user.clear(minPriceInput)
    await user.click(screen.getByRole("button", { name: "Salvează configurația" }))

    expect(updateMutate).toHaveBeenCalledTimes(1)
    const payload = updateMutate.mock.calls[0][0]
    expect(payload.minPrice).toBeNull()
  })

  it("rejects a negative leadTimeDays with the real form", async () => {
    const updateMutate = vi.fn()
    mockHappyPath({ updateMutate })
    renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

    const leadTimeInput = screen.getByLabelText("Prag last-minute (zile până la check-in)")
    // Typing "-1" char-by-char into a controlled number input is unreliable under jsdom
    // (userEvent drops the leading "-"), and mixing userEvent with a raw fireEvent.change
    // on the same field causes an act() timing mismatch — so both the value and the
    // submit go through fireEvent here instead of userEvent.
    fireEvent.change(leadTimeInput, { target: { value: "-1" } })
    fireEvent.submit(screen.getByRole("button", { name: "Salvează configurația" }).closest("form")!)

    expect(await screen.findByText("Nu poate fi negativ")).toBeInTheDocument()
    expect(updateMutate).not.toHaveBeenCalled()
  })

  describe("local events form", () => {
    it("keeps 'Adaugă eveniment' disabled until label, dates, and a positive multiplier are all set", async () => {
      mockHappyPath()
      const user = userEvent.setup()
      const { container } = renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      const addButton = screen.getByRole("button", { name: "Adaugă eveniment" })
      expect(addButton).toBeDisabled()

      await user.type(screen.getByPlaceholderText("Etichetă (ex: Festival local)"), "Festival")
      expect(addButton).toBeDisabled()

      const dateInputs = container.querySelectorAll('input[type="date"]')
      await user.type(dateInputs[0], "2026-09-10")
      await user.type(dateInputs[1], "2026-09-15")
      expect(addButton).toBeDisabled()

      await user.type(screen.getByPlaceholderText("Multiplicator"), "1.5")
      expect(addButton).toBeEnabled()
    })

    it("shows an inline error and keeps the button disabled when endDate is before startDate", async () => {
      mockHappyPath()
      const user = userEvent.setup()
      const { container } = renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.type(screen.getByPlaceholderText("Etichetă (ex: Festival local)"), "Festival")
      const dateInputs = container.querySelectorAll('input[type="date"]')
      await user.type(dateInputs[0], "2026-09-15")
      await user.type(dateInputs[1], "2026-09-10")
      await user.type(screen.getByPlaceholderText("Multiplicator"), "1.5")

      expect(screen.getByText("Data de sfârșit nu poate fi înainte de data de început.")).toBeInTheDocument()
      expect(screen.getByRole("button", { name: "Adaugă eveniment" })).toBeDisabled()
    })

    it("shows an inline error when the price multiplier is zero or negative", async () => {
      mockHappyPath()
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.type(screen.getByPlaceholderText("Multiplicator"), "0")

      expect(screen.getByText("Multiplicatorul trebuie să fie mai mare ca 0.")).toBeInTheDocument()
    })

    it("sends propertyId (not city) when adding an event with the default 'this apartment' scope", async () => {
      const createMutate = vi.fn()
      mockHappyPath()
      vi.mocked(useCreateLocalEvent).mockReturnValue({ mutate: createMutate, isPending: false } as never)
      const user = userEvent.setup()
      const { container } = renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.type(screen.getByPlaceholderText("Etichetă (ex: Festival local)"), "Festival")
      const dateInputs = container.querySelectorAll('input[type="date"]')
      await user.type(dateInputs[0], "2026-09-10")
      await user.type(dateInputs[1], "2026-09-15")
      await user.type(screen.getByPlaceholderText("Multiplicator"), "1.5")
      await user.click(screen.getByRole("button", { name: "Adaugă eveniment" }))

      expect(createMutate).toHaveBeenCalledTimes(1)
      const payload = createMutate.mock.calls[0][0]
      expect(payload.propertyId).toBe("prop-1")
      expect(payload.city).toBeUndefined()
    })

    it("sends city (not propertyId) when the scope is switched to the whole city", async () => {
      const createMutate = vi.fn()
      mockHappyPath()
      vi.mocked(useCreateLocalEvent).mockReturnValue({ mutate: createMutate, isPending: false } as never)
      const user = userEvent.setup()
      const { container } = renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.type(screen.getByPlaceholderText("Etichetă (ex: Festival local)"), "Festival")
      const dateInputs = container.querySelectorAll('input[type="date"]')
      await user.type(dateInputs[0], "2026-09-10")
      await user.type(dateInputs[1], "2026-09-15")
      await user.type(screen.getByPlaceholderText("Multiplicator"), "1.5")

      const scopeCombobox = screen.getByRole("combobox")
      await user.click(scopeCombobox)
      const cityOption = await screen.findByRole("option", { name: "Tot orașul Cluj-Napoca" })
      await user.click(cityOption)

      await user.click(screen.getByRole("button", { name: "Adaugă eveniment" }))

      expect(createMutate).toHaveBeenCalledTimes(1)
      const payload = createMutate.mock.calls[0][0]
      expect(payload.city).toBe("Cluj-Napoca")
      expect(payload.propertyId).toBeUndefined()
    })
  })
})
