import type { Metadata } from "next"
import { CleaningTasksView } from "./cleaning-tasks-view"

export const metadata: Metadata = {
  title: "Curățenie",
}

export default function CleaningPage() {
  return <CleaningTasksView />
}
