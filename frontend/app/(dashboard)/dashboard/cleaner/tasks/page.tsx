import type { Metadata } from "next"
import { MyCleaningTasksView } from "./my-cleaning-tasks-view"

export const metadata: Metadata = {
  title: "Sarcinile mele",
}

export default function MyCleaningTasksPage() {
  return <MyCleaningTasksView />
}
