import { describe, expect, it, vi } from "vitest"
import { screen, renderWithProviders, userEvent, waitFor } from "@/test/utils"
import { ChatWidget } from "./chat-widget"
import { assistantApi } from "@/lib/api/assistant"

vi.mock("@/lib/api/assistant", () => ({
  assistantApi: {
    chat: vi.fn(),
  },
}))

async function openWidgetAndAsk(question: string) {
  const user = userEvent.setup()
  renderWithProviders(<ChatWidget />)

  await user.click(screen.getByRole("button", { name: "Deschide chat" }))
  await user.type(screen.getByPlaceholderText("Scrie o întrebare..."), question)
  await user.click(screen.getByRole("button", { name: "Trimite mesaj" }))

  return user
}

describe("ChatWidget", () => {
  it("shows the assistant's reply and sends only the real conversation (not the canned greeting)", async () => {
    vi.mocked(assistantApi.chat).mockResolvedValue({ message: "Check-in-ul variază pe proprietate." })

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
})
