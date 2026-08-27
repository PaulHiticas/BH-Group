import type { Metadata } from "next"
import { AssistantChatsView } from "./assistant-chats-view"

export const metadata: Metadata = {
  title: "Conversații asistent AI",
}

export default function AssistantChatsPage() {
  return <AssistantChatsView />
}
