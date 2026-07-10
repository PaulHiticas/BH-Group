"use client"

import Link from "next/link"
import { CalendarDays, Mail, Phone, ShieldCheck, ShieldOff } from "lucide-react"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { buttonVariants } from "@/components/ui/button"
import { useCurrentUser } from "@/hooks/use-current-user"
import { ROLE_LABELS, USER_STATUS_BADGE_VARIANT, USER_STATUS_LABELS } from "@/lib/roles"
import { cn } from "@/lib/utils"

function initials(firstName: string, lastName: string) {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()
}

export function ProfileView() {
  const { data: user, isLoading } = useCurrentUser()

  if (isLoading || !user) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col gap-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Profilul meu</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Informațiile contului tău de administrare.
        </p>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center gap-4">
          <div className="flex size-16 items-center justify-center rounded-full bg-primary text-xl font-semibold text-primary-foreground">
            {initials(user.firstName, user.lastName)}
          </div>
          <div>
            <CardTitle className="text-lg">
              {user.firstName} {user.lastName}
            </CardTitle>
            <CardDescription className="mt-1 flex flex-wrap items-center gap-2">
              <Badge variant="secondary">{ROLE_LABELS[user.role]}</Badge>
              <Badge variant={USER_STATUS_BADGE_VARIANT[user.status]}>
                {USER_STATUS_LABELS[user.status]}
              </Badge>
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 border-t border-border/60 pt-4">
          <div className="flex items-center gap-2.5 text-sm">
            <Mail className="size-4 text-muted-foreground" />
            <span>{user.email}</span>
          </div>
          {user.phone && (
            <div className="flex items-center gap-2.5 text-sm">
              <Phone className="size-4 text-muted-foreground" />
              <span>{user.phone}</span>
            </div>
          )}
          <div className="flex items-center gap-2.5 text-sm">
            <CalendarDays className="size-4 text-muted-foreground" />
            <span>Membru din {new Date(user.createdAt).toLocaleDateString("ro-RO")}</span>
          </div>
          <div className="flex items-center gap-2.5 text-sm">
            {user.mfaEnabled ? (
              <>
                <ShieldCheck className="size-4 text-emerald-600 dark:text-emerald-400" />
                <span>Autentificare în doi pași activă</span>
              </>
            ) : (
              <>
                <ShieldOff className="size-4 text-amber-600 dark:text-amber-400" />
                <span>Autentificare în doi pași dezactivată</span>
              </>
            )}
          </div>
        </CardContent>
      </Card>

      <Link href="/dashboard/settings" className={cn(buttonVariants({ variant: "outline" }), "w-fit")}>
        Administrează securitatea contului
      </Link>
    </div>
  )
}
