import { describe, expect, it, vi, beforeEach } from "vitest"
import { screen, renderWithProviders, userEvent } from "@/test/utils"
import { mockRouter } from "@/test/mocks/next-navigation"
import { AssistantChatsView } from "./assistant-chats-view"
import { useAssistantChats } from "@/hooks/use-assistant-chats"

vi.mock("@/hooks/use-assistant-chats", () => ({
  useAssistantChats: vi.fn(),
}))

const onePage = {
  content: [
    {
      id: "chat-1",
      guestName: "Ion Popescu",
      guestEmail: "ion@example.com",
      status: "OPEN" as const,
      lastMessageAt: "2026-01-01T10:00:00Z",
      createdAt: "2026-01-01T09:00:00Z",
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
}

describe("AssistantChatsView", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("shows an empty state when there are no chats", () => {
    vi.mocked(useAssistantChats).mockReturnValue({
      data: { ...onePage, content: [], totalElements: 0 },
      isLoading: false,
    } as never)

    renderWithProviders(<AssistantChatsView />)

    expect(screen.getByText("Nicio conversație încă.")).toBeInTheDocument()
  })

  it("lists a chat with the guest name/email and status, and navigates to its detail on click", async () => {
    vi.mocked(useAssistantChats).mockReturnValue({ data: onePage, isLoading: false } as never)
    const user = userEvent.setup()
    renderWithProviders(<AssistantChatsView />)

    expect(screen.getByText("Ion Popescu")).toBeInTheDocument()
    expect(screen.getByText("ion@example.com")).toBeInTheDocument()
    expect(screen.getByText("Deschisă")).toBeInTheDocument()

    await user.click(screen.getByText("Ion Popescu"))

    expect(mockRouter.push).toHaveBeenCalledWith("/dashboard/assistant-chats/chat-1")
  })

  it("passes the selected status filter to the hook", async () => {
    vi.mocked(useAssistantChats).mockReturnValue({ data: onePage, isLoading: false } as never)
    const user = userEvent.setup()
    renderWithProviders(<AssistantChatsView />)

    await user.click(screen.getByRole("combobox"))
    await user.click(screen.getByRole("option", { name: "Rezolvate" }))

    expect(useAssistantChats).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: "RESOLVED" })
    )
  })
})
