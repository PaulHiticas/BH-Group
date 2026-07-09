import type { Metadata } from "next"
import { BookingSearchView } from "./search-view"

export const metadata: Metadata = {
  title: "Caută o proprietate",
}

export default function PublicBookingSearchPage() {
  return <BookingSearchView />
}
