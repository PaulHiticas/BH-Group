"use client"

import { LogOut, User } from "lucide-react"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Skeleton } from "@/components/ui/skeleton"
import { ThemeToggle } from "@/components/layout/theme-toggle"
import { DashboardMobileNav } from "@/components/layout/dashboard-mobile-nav"
import { useCurrentUser } from "@/hooks/use-current-user"
import { useLogout } from "@/hooks/use-auth"
import { ROLE_LABELS } from "@/lib/roles"

function initials(firstName: string, lastName: string) {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()
}

export function DashboardTopbar() {
  const { data: user, isLoading } = useCurrentUser()
  const logout = useLogout()

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border/60 bg-background/80 px-6 backdrop-blur">
      <DashboardMobileNav />

      <div className="ml-auto flex items-center gap-2">
        <ThemeToggle />

        {isLoading || !user ? (
          <Skeleton className="size-9 rounded-full" />
        ) : (
          <DropdownMenu>
            <DropdownMenuTrigger
              render={
                <button className="flex items-center gap-2 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring" />
              }
            >
              <Avatar className="size-9">
                <AvatarFallback>
                  {initials(user.firstName, user.lastName)}
                </AvatarFallback>
              </Avatar>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel className="flex flex-col gap-0.5">
                <span className="font-medium">{user.firstName} {user.lastName}</span>
                <span className="text-xs font-normal text-muted-foreground">{user.email}</span>
              </DropdownMenuLabel>
              <div className="px-2 pb-1">
                <Badge variant="secondary">{ROLE_LABELS[user.role]}</Badge>
              </div>
              <DropdownMenuSeparator />
              <DropdownMenuItem disabled>
                <User className="size-4" />
                Profil
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                variant="destructive"
                onClick={() => logout.mutate()}
              >
                <LogOut className="size-4" />
                Deconectare
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>
    </header>
  )
}
