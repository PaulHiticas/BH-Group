import { describe, expect, it, vi, beforeEach } from "vitest"
import { screen, renderWithProviders, userEvent } from "@/test/utils"
import { createThreadSchema, OwnerThreadsView } from "./owner-threads-view"
import { useOwnerProperties } from "@/hooks/use-owner"
import { useCreateOwnerThread, useMyOwnerThreads } from "@/hooks/use-owner-threads"

vi.mock("@/hooks/use-owner", () => ({
  useOwnerProperties: vi.fn(),
}))

vi.mock("@/hooks/use-owner-threads", () => ({
  useCreateOwnerThread: vi.fn(),
  useMyOwnerThreads: vi.fn(),
}))

// ---------------------------------------------------------------------------
// Pure schema tests
// ---------------------------------------------------------------------------

describe("createThreadSchema", () => {
  it("requires a non-empty subject", () => {
    const result = createThreadSchema.safeParse({ subject: "", propertyId: "__none__", body: "Mesaj" })
    expect(result.success).toBe(false)
  })

  it("requires a non-empty body", () => {
    const result = createThreadSchema.safeParse({ subject: "Subiect", propertyId: "__none__", body: "" })
    expect(result.success).toBe(false)
  })

  it("accepts a valid subject and body", () => {
    const result = createThreadSchema.safeParse({ subject: "Subiect", propertyId: "__none__", body: "Mesaj" })
    expect(result.success).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// Rendered component tests
// ---------------------------------------------------------------------------

const emptyPage = { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0, first: true, last: true }

function mockHooks(createMutate = vi.fn()) {
  vi.mocked(useOwnerProperties).mockReturnValue({ data: emptyPage, isLoading: false } as never)
  vi.mocked(useMyOwnerThreads).mockReturnValue({
    data: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true },
    isLoading: false,
  } as never)
  vi.mocked(useCreateOwnerThread).mockReturnValue({ mutate: createMutate, isPending: false } as never)
}

describe("OwnerThreadsView — new request form", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("shows validation errors and does not submit when subject and message are empty", async () => {
    const createMutate = vi.fn()
    mockHooks(createMutate)
    const user = userEvent.setup()
    renderWithProviders(<OwnerThreadsView />)

    await user.click(screen.getByRole("button", { name: "Cerere nouă" }))
    await user.click(screen.getByRole("button", { name: "Trimite cererea" }))

    expect(await screen.findByText("Subiectul este obligatoriu")).toBeInTheDocument()
    expect(screen.getByText("Mesajul este obligatoriu")).toBeInTheDocument()
    expect(createMutate).not.toHaveBeenCalled()
  })

  it("submits a general request (no property) with the entered subject and body", async () => {
    const createMutate = vi.fn()
    mockHooks(createMutate)
    const user = userEvent.setup()
    renderWithProviders(<OwnerThreadsView />)

    await user.click(screen.getByRole("button", { name: "Cerere nouă" }))
    await user.type(screen.getByLabelText("Subiect"), "Reparație boiler")
    await user.type(screen.getByLabelText("Mesaj"), "Boilerul nu funcționează")
    await user.click(screen.getByRole("button", { name: "Trimite cererea" }))

    expect(createMutate).toHaveBeenCalledTimes(1)
    const payload = createMutate.mock.calls[0][0]
    expect(payload.subject).toBe("Reparație boiler")
    expect(payload.body).toBe("Boilerul nu funcționează")
    expect(payload.propertyId).toBeUndefined()
  })
})
