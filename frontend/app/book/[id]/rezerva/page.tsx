import type { Metadata } from "next"
import { BookingContent } from "./booking-content"

export const metadata: Metadata = {
  title: "Rezervă",
}

export default async function BookingPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return <BookingContent id={id} />
}
