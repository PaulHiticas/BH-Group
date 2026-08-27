import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen, renderWithProviders, userEvent, waitFor } from "@/test/utils"
import { ChatWidget } from "./chat-widget"
import { assistantApi } from "@/lib/api/assistant"

vi.mock("@/lib/api/assistant", () => ({
  assistantApi: {
    chat: vi.fn(),
    handoff: vi.fn(),
    getHandoffMessages: vi.fn(),
  },
}))

async function openWidget() {
  const user = userEvent.setup()
  renderWithProviders(<ChatWidget />)
  await user.click(screen.getByRole("button", { name: "Deschide chat" }))
  return user
}

async function openWidgetAndAsk(question: string) {
  const user = await openWidget()
  await user.type(screen.getByPlaceholderText("Scrie o întrebare..."), question)
  await user.click(screen.getByRole("button", { name: "Trimite mesaj" }))
  return user
}

describe("ChatWidget", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("shows the assistant's reply and sends only the real conversation (not the canned greeting)", async () => {
    vi.mocked(assistantApi.chat).mockResolvedValue({ message: "Check-in-ul variază pe proprietate.", needsHuman: false })

    await openWidgetAndAsk("Care e ora de check-in?")

    await waitFor(() => {
      expect(screen.getByText("Check-in-ul variază pe proprietate.")).toBeInTheDocument()
    })

    expect(assistantApi.chat).toHaveBeenCalledWith([
      { role: "user", content: "Care e ora de check-in?" },
    ])
  })

  it("shows a contact fallback message when the request fails", async () => {
    vi.mocked(assistantApi.chat).mockRejectedValue(new Error("network down"))

    await openWidgetAndAsk("Salut")

    await waitFor(() => {
      expect(screen.getByText(/Nu am putut trimite mesajul/)).toBeInTheDocument()
    })
  })

  it("starts a handoff when the visitor clicks 'Vorbește cu o persoană din echipă', even with no prior FAQ messages", async () => {
    vi.mocked(assistantApi.handoff).mockResolvedValue({ publicToken: "tok-123" })
    vi.mocked(assistantApi.getHandoffMessages).mockResolvedValue([])

    const user = await openWidget()
    await user.click(screen.getByRole("button", { name: "Vorbește cu o persoană din echipă" }))

    await waitFor(() => {
      expect(screen.getByText(/Te conectez cu un coleg/)).toBeInTheDocument()
    })
    expect(assistantApi.handoff).toHaveBeenCalledWith({ messages: [] })
    // Input is gone once in handoff mode - the visitor only reads staff replies from here.
    expect(screen.queryByPlaceholderText("Scrie o întrebare...")).not.toBeInTheDocument()
  })

  it("shows a staff reply once the poll picks it up after handoff", async () => {
    vi.mocked(assistantApi.handoff).mockResolvedValue({ publicToken: "tok-456" })
    vi.mocked(assistantApi.getHandoffMessages).mockResolvedValue([
      { id: "m1", senderType: "STAFF", body: "Bună, cu ce te pot ajuta?", createdAt: "2026-01-01T10:00:00Z" },
    ])

    const user = await openWidget()
    await user.click(screen.getByRole("button", { name: "Vorbește cu o persoană din echipă" }))

    await waitFor(() => {
      expect(screen.getByText("Bună, cu ce te pot ajuta?")).toBeInTheDocument()
    })
  })

  it("escalates immediately on a written request to talk to a human - never calls chat, never waits for the threshold", async () => {
    vi.mocked(assistantApi.handoff).mockResolvedValue({ publicToken: "tok-111" })
    vi.mocked(assistantApi.getHandoffMessages).mockResolvedValue([])

    await openWidgetAndAsk("Vreau să vorbesc cu o persoană")

    await waitFor(() => {
      expect(screen.getByText(/Te conectez cu un coleg/)).toBeInTheDocument()
    })
    expect(assistantApi.handoff).toHaveBeenCalledWith({
      messages: [{ role: "user", content: "Vreau să vorbesc cu o persoană" }],
    })
    expect(assistantApi.chat).not.toHaveBeenCalled()
  })

  it("also recognizes an English written request to talk to a human", async () => {
    vi.mocked(assistantApi.handoff).mockResolvedValue({ publicToken: "tok-222" })
    vi.mocked(assistantApi.getHandoffMessages).mockResolvedValue([])

    await openWidgetAndAsk("Can I talk to a human please?")

    await waitFor(() => {
      expect(assistantApi.handoff).toHaveBeenCalledTimes(1)
    })
    expect(assistantApi.chat).not.toHaveBeenCalled()
  })

  it("auto-escalates to a human after two needsHuman replies, without a manual click", async () => {
    vi.mocked(assistantApi.chat).mockResolvedValue({
      message: "Nu pot confirma detalii despre rezervarea ta.",
      needsHuman: true,
    })
    vi.mocked(assistantApi.handoff).mockResolvedValue({ publicToken: "tok-789" })
    vi.mocked(assistantApi.getHandoffMessages).mockResolvedValue([])

    const user = await openWidgetAndAsk("Care e statusul rezervării mele #123?")
    await waitFor(() => expect(assistantApi.chat).toHaveBeenCalledTimes(1))
    expect(assistantApi.handoff).not.toHaveBeenCalled()

    await user.type(screen.getByPlaceholderText("Scrie o întrebare..."), "Dar mai exact?")
    await user.click(screen.getByRole("button", { name: "Trimite mesaj" }))

    await waitFor(() => {
      expect(assistantApi.handoff).toHaveBeenCalledTimes(1)
    })
    await waitFor(() => {
      expect(screen.getByText(/Te conectez cu un coleg/)).toBeInTheDocument()
    })
  })
})
