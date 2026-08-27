import { describe, expect, it, vi } from "vitest"
import { screen, render, userEvent } from "@/test/utils"
import { AssistantChatMessages } from "./assistant-chat-messages"
import type { AssistantChatMessageResponse } from "@/lib/api/assistant-chats"

const messages: AssistantChatMessageResponse[] = [
  { id: "m1", senderType: "GUEST", body: "Salut, am nevoie de ajutor", createdAt: "2026-01-01T10:00:00Z" },
  { id: "m2", senderType: "AI", body: "Nu sunt sigur, revine un coleg.", createdAt: "2026-01-01T10:01:00Z" },
]

describe("AssistantChatMessages", () => {
  it("renders each message with its sender label", () => {
    render(
      <AssistantChatMessages messages={messages} isLoading={false} onSend={vi.fn()} isSending={false} />
    )

    expect(screen.getByText("Salut, am nevoie de ajutor")).toBeInTheDocument()
    expect(screen.getByText(/Vizitator/)).toBeInTheDocument()
    expect(screen.getByText(/Asistent AI/)).toBeInTheDocument()
  })

  it("shows an empty-state message when there are no messages yet", () => {
    render(<AssistantChatMessages messages={[]} isLoading={false} onSend={vi.fn()} isSending={false} />)
    expect(screen.getByText("Niciun mesaj încă.")).toBeInTheDocument()
  })

  it("sends the typed reply and clears the input", async () => {
    const onSend = vi.fn()
    const user = userEvent.setup()
    render(<AssistantChatMessages messages={messages} isLoading={false} onSend={onSend} isSending={false} />)

    const textbox = screen.getByPlaceholderText("Scrie un răspuns vizitatorului...")
    await user.type(textbox, "Bună, cu ce te pot ajuta?")
    await user.click(screen.getByRole("button", { name: "Trimite mesaj" }))

    expect(onSend).toHaveBeenCalledWith("Bună, cu ce te pot ajuta?")
    expect(textbox).toHaveValue("")
  })

  it("does not send an empty or whitespace-only reply", async () => {
    const onSend = vi.fn()
    const user = userEvent.setup()
    render(<AssistantChatMessages messages={messages} isLoading={false} onSend={onSend} isSending={false} />)

    await user.type(screen.getByPlaceholderText("Scrie un răspuns vizitatorului..."), "   ")
    await user.click(screen.getByRole("button", { name: "Trimite mesaj" }))

    expect(onSend).not.toHaveBeenCalled()
  })
})
