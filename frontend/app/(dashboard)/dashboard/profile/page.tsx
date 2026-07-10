import type { Metadata } from "next"
import { ProfileView } from "./profile-view"

export const metadata: Metadata = {
  title: "Profilul meu",
}

export default function ProfilePage() {
  return <ProfileView />
}
