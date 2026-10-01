import { describe, expect, it, vi, beforeEach } from "vitest"
import { fireEvent } from "@testing-library/react"
import { screen, renderWithProviders, userEvent } from "@/test/utils"
import { dynamicPricingConfigSchema, PricingAdminSection } from "./pricing-admin-section"
import type {
  AiPricingRecommendationResponse,
  DynamicPricingConfigResponse,
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
import { ApiError } from "@/lib/api/types"

vi.mock("@/hooks/use-pricing", () => ({
  usePricingConfig: vi.fn(),
  useUpdatePricingConfig: vi.fn(),
  useAiPricingRecommendation: vi.fn(),
  usePricingBreakdown: vi.fn(),
  useMergedLocalEvents: vi.fn(),
  useCreateLocalEvent: vi.fn(),
  useDeleteLocalEvent: vi.fn(),
}))

vi.mock("@/hooks/use-current-user", () => ({
  useCurrentUser: vi.fn(),
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

const baseRecommendation: AiPricingRecommendationResponse = {
  propertyId: "prop-1",
  currency: "RON",
  recommendation: {
    enabled: true,
    minPrice: 180,
    maxPrice: 420,
    occupancyWindowDays: 30,
    occupancyMultiplierMin: 0.85,
    occupancyMultiplierMax: 1.45,
    leadTimeDays: 5,
    leadTimeMultiplier: 0.92,
  },
  confidence: "MEDIUM",
  summary: "Ocupare moderată, recomand o bandă mai largă.",
  reasons: ["Ocupare 40% în 30 de zile.", "Tarif mediu sub tariful de bază."],
  metricsUsed: {
    windowDays: 30,
    bookedNights: 12,
    windowNights: 30,
    occupancyRate: 0.4,
    averageDailyRate: 190,
    basePricePerNight: 200,
    seasonalRatesConfigured: 2,
    upcomingLocalEvents: 1,
  },
  warnings: ["Prețul minim scade sub tariful mediu realizat."],
  missingData: ["Niciun eveniment local viitor înregistrat."],
  generatedAt: "2026-10-01T09:30:00Z",
}

/**
 * Stands in for the mutation. The returned object must be stable across
 * renders: the component keeps `reset` in an effect's dependency list, so a
 * fresh object per render would loop.
 */
function mockAiRecommendation(options: { response?: AiPricingRecommendationResponse; isPending?: boolean } = {}) {
  const mutate = vi.fn(
    (_variables: unknown, handlers?: { onSuccess?: (data: AiPricingRecommendationResponse) => void }) => {
      if (options.response) handlers?.onSuccess?.(options.response)
    }
  )
  const value = { mutate, isPending: options.isPending ?? false, reset: vi.fn() }
  vi.mocked(useAiPricingRecommendation).mockReturnValue(value as never)
  return value
}

function mockHappyPath(
  overrides: {
    updateMutate?: ReturnType<typeof vi.fn>
    role?: string | null
    recommendation?: AiPricingRecommendationResponse
    aiPending?: boolean
  } = {}
) {
  vi.mocked(usePricingConfig).mockReturnValue({ data: baseConfig, isLoading: false } as never)
  vi.mocked(useUpdatePricingConfig).mockReturnValue({
    mutate: overrides.updateMutate ?? vi.fn(),
    isPending: false,
  } as never)
  vi.mocked(useCurrentUser).mockReturnValue({
    data: overrides.role === null ? undefined : { role: overrides.role ?? "ADMINISTRATOR" },
  } as never)
  mockAiRecommendation({ response: overrides.recommendation, isPending: overrides.aiPending })
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
  describe("AI pricing recommendation", () => {
    const AI_BUTTON = { name: "Recomandare AI" }

    it("offers the button to roles that manage pricing and hides it from the rest", () => {
      mockHappyPath({ role: "ADMINISTRATOR" })
      const { unmount } = renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)
      expect(screen.getByRole("button", AI_BUTTON)).toBeInTheDocument()
      unmount()

      mockHappyPath({ role: "OWNER" })
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)
      expect(screen.queryByRole("button", AI_BUTTON)).not.toBeInTheDocument()
    })

    it("fills the form from the recommendation without saving anything", async () => {
      const updateMutate = vi.fn()
      mockHappyPath({ updateMutate, recommendation: baseRecommendation })
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.click(screen.getByRole("button", AI_BUTTON))

      expect(screen.getByLabelText("Preț minim (plafon siguranță)")).toHaveValue(180)
      expect(screen.getByLabelText("Preț maxim (plafon siguranță)")).toHaveValue(420)
      expect(screen.getByLabelText("Fereastră ocupare (zile)")).toHaveValue(30)
      expect(screen.getByLabelText("Multiplicator ocupare — minim")).toHaveValue(0.85)
      expect(screen.getByLabelText("Multiplicator ocupare — maxim")).toHaveValue(1.45)
      expect(screen.getByLabelText("Prag last-minute (zile până la check-in)")).toHaveValue(5)
      expect(screen.getByLabelText("Multiplicator last-minute")).toHaveValue(0.92)
      // Saving stays the only thing that persists, and it was never called.
      expect(updateMutate).not.toHaveBeenCalled()
    })

    it("marks the form as having unsaved changes once a recommendation is applied", async () => {
      mockHappyPath({ recommendation: baseRecommendation })
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      expect(screen.queryByText("Ai modificări nesalvate")).not.toBeInTheDocument()
      await user.click(screen.getByRole("button", AI_BUTTON))
      expect(screen.getByText("Ai modificări nesalvate")).toBeInTheDocument()
    })

    it("shows the summary, confidence, reasons, metrics, missing data and warnings", async () => {
      mockHappyPath({ recommendation: baseRecommendation })
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.click(screen.getByRole("button", AI_BUTTON))

      expect(screen.getByText("Ocupare moderată, recomand o bandă mai largă.")).toBeInTheDocument()
      expect(screen.getByText("Încredere: Medie")).toBeInTheDocument()
      expect(screen.getByText("Ocupare 40% în 30 de zile.")).toBeInTheDocument()
      expect(screen.getByText("Tarif mediu sub tariful de bază.")).toBeInTheDocument()
      expect(screen.getByText("12 / 30 (40%)")).toBeInTheDocument()
      expect(screen.getByText("Niciun eveniment local viitor înregistrat.")).toBeInTheDocument()
      expect(screen.getByText("Prețul minim scade sub tariful mediu realizat.")).toBeInTheDocument()
    })

    it("compares the previous values against the recommended ones", async () => {
      mockHappyPath({ recommendation: baseRecommendation })
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.click(screen.getByRole("button", AI_BUTTON))

      const row = screen.getByRole("cell", { name: "Preț minim" }).closest("tr")!
      expect(row).toHaveTextContent("100 RON")
      expect(row).toHaveTextContent("180 RON")

      // maxPrice was unset before, so the old column has to say so rather than
      // render an empty cell.
      const maxRow = screen.getByRole("cell", { name: "Preț maxim" }).closest("tr")!
      expect(maxRow).toHaveTextContent("—")
      expect(maxRow).toHaveTextContent("420 RON")
    })

    it("restores the values from before the recommendation and drops the card", async () => {
      mockHappyPath({ recommendation: baseRecommendation })
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.click(screen.getByRole("button", AI_BUTTON))
      expect(screen.getByLabelText("Preț minim (plafon siguranță)")).toHaveValue(180)

      await user.click(screen.getByRole("button", { name: "Revino la valorile anterioare" }))

      expect(screen.getByLabelText("Preț minim (plafon siguranță)")).toHaveValue(100)
      expect(screen.getByLabelText("Fereastră ocupare (zile)")).toHaveValue(14)
      expect(screen.queryByText("Ocupare moderată, recomand o bandă mai largă.")).not.toBeInTheDocument()
      expect(screen.queryByText("Ai modificări nesalvate")).not.toBeInTheDocument()
    })

    it("ignores a recommendation addressed to another property", async () => {
      mockHappyPath({ recommendation: { ...baseRecommendation, propertyId: "prop-2" } })
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.click(screen.getByRole("button", AI_BUTTON))

      expect(screen.getByLabelText("Preț minim (plafon siguranță)")).toHaveValue(100)
      expect(screen.queryByText("Ocupare moderată, recomand o bandă mai largă.")).not.toBeInTheDocument()
    })

    it("blocks a second request while one is still running", () => {
      mockHappyPath({ aiPending: true })
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      const button = screen.getByRole("button", { name: "Se generează..." })
      expect(button).toBeDisabled()
      expect(screen.getByText("Se analizează ocuparea și tarifele proprietății...")).toBeInTheDocument()
    })

    it("reports a 503 in Romanian and says the configuration is unchanged", () => {
      mockHappyPath()
      vi.mocked(useAiPricingRecommendation).mockReturnValue({
        mutate: vi.fn(),
        isPending: false,
        isError: true,
        error: new ApiError(503, {
          success: false,
          errorCode: "PRICING_AI_UNAVAILABLE",
          message: "Recomandarea de preț nu a putut fi generată.",
          timestamp: "",
          path: "",
        }),
        reset: vi.fn(),
      } as never)
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      expect(screen.getByText(/Recomandarea AI nu este disponibilă acum/)).toBeInTheDocument()
      expect(screen.getByLabelText("Preț minim (plafon siguranță)")).toHaveValue(100)
    })

    it("leaves the form untouched when the request fails", async () => {
      // No response configured, so onSuccess never fires - the failure path.
      mockHappyPath()
      const user = userEvent.setup()
      renderWithProviders(<PricingAdminSection propertyId="prop-1" city="Cluj-Napoca" />)

      await user.click(screen.getByRole("button", AI_BUTTON))

      expect(screen.getByLabelText("Preț minim (plafon siguranță)")).toHaveValue(100)
      expect(screen.getByLabelText("Fereastră ocupare (zile)")).toHaveValue(14)
      expect(screen.getByLabelText("Multiplicator ocupare — maxim")).toHaveValue(1.3)
      expect(screen.queryByText("Ai modificări nesalvate")).not.toBeInTheDocument()
      expect(screen.getByRole("button", { name: "Salvează configurația" })).toBeEnabled()
    })
  })
})
