"use client"

import { useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { KeyRound, Plus, ShieldCheck, ShieldOff, UserPlus } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useCreateUser, useUpdateUserStatus, useUsers } from "@/hooks/use-users"
import { useCurrentUser } from "@/hooks/use-current-user"
import { ALL_ROLES, ROLE_LABELS, USER_STATUS_BADGE_VARIANT, USER_STATUS_LABELS } from "@/lib/roles"
import type { UserStatus } from "@/lib/api/types"

const createUserSchema = z.object({
  firstName: z.string().min(1, "Prenumele este obligatoriu").max(100),
  lastName: z.string().min(1, "Numele este obligatoriu").max(100),
  email: z.string().min(1, "Emailul este obligatoriu").email("Adresă de email invalidă"),
  password: z
    .string()
    .min(8, "Parola trebuie să aibă cel puțin 8 caractere")
    .regex(/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, "Parola trebuie să conțină literă mare, literă mică și cifră"),
  phone: z.string().max(30).optional(),
  role: z.enum(["SUPER_ADMIN", "ADMINISTRATOR"]),
})

type CreateUserValues = z.infer<typeof createUserSchema>

function CreateUserDialog() {
  const [open, setOpen] = useState(false)
  const createUser = useCreateUser()
  const form = useForm<CreateUserValues>({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      password: "",
      phone: "",
      role: "ADMINISTRATOR",
    },
  })

  function onSubmit(values: CreateUserValues) {
    createUser.mutate(
      {
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        password: values.password,
        phone: values.phone || undefined,
        role: values.role,
      },
      {
        onSuccess: () => {
          form.reset()
          setOpen(false)
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button />}>
        <Plus className="size-4" />
        Adaugă membru
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Cont nou de administrare</DialogTitle>
          <DialogDescription>
            Stabilești tu parola inițială și i-o transmiți colegului direct (telefon, mesaj privat).
            La prima autentificare o poate schimba, iar 2FA îl activează singur din contul lui, din
            Setări — niciodată nu se folosește codul tău.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="firstName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Prenume</FormLabel>
                    <FormControl>
                      <Input placeholder="Ion" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="lastName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nume</FormLabel>
                    <FormControl>
                      <Input placeholder="Popescu" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input type="email" placeholder="nume@bhgroup.io" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Parolă inițială</FormLabel>
                  <FormControl>
                    <Input type="text" placeholder="Minim 8 caractere, literă mare + mică + cifră" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Telefon (opțional)</FormLabel>
                    <FormControl>
                      <Input placeholder="07xx xxx xxx" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="role"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Rol</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {ALL_ROLES.map((r) => (
                          <SelectItem key={r} value={r}>
                            {ROLE_LABELS[r]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <Button type="submit" className="w-full" disabled={createUser.isPending}>
              <UserPlus className="size-4" />
              {createUser.isPending ? "Se creează..." : "Creează contul"}
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}

export function UsersView() {
  const { data: me } = useCurrentUser()
  const [page, setPage] = useState(0)
  const { data, isLoading } = useUsers({ page })
  const updateStatus = useUpdateUserStatus()

  const isSuperAdmin = me?.role === "SUPER_ADMIN"

  function toggleStatus(id: string, current: UserStatus) {
    const next: UserStatus = current === "ACTIVE" ? "SUSPENDED" : "ACTIVE"
    updateStatus.mutate({ id, status: next })
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Echipă</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Administrează conturile colegilor care au acces în platformă.
          </p>
        </div>
        {isSuperAdmin && <CreateUserDialog />}
      </div>

      {!isSuperAdmin && (
        <div className="flex items-center gap-2 rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-600 dark:text-amber-400">
          <KeyRound className="size-4" />
          Doar un Super Admin poate crea sau modifica alte conturi.
        </div>
      )}

      {isLoading ? (
        <Skeleton className="h-96 w-full" />
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Nu au fost găsiți utilizatori.</p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border border-border/60">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nume</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Rol</TableHead>
                  <TableHead>2FA</TableHead>
                  <TableHead>Status</TableHead>
                  {isSuperAdmin && <TableHead className="text-right">Acțiuni</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell className="font-medium">
                      {u.firstName} {u.lastName}
                      {u.id === me?.id && (
                        <span className="ml-2 text-xs text-muted-foreground">(tu)</span>
                      )}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{u.email}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{ROLE_LABELS[u.role]}</Badge>
                    </TableCell>
                    <TableCell>
                      {u.mfaEnabled ? (
                        <span className="inline-flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400">
                          <ShieldCheck className="size-3.5" /> Activ
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                          <ShieldOff className="size-3.5" /> Inactiv
                        </span>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={USER_STATUS_BADGE_VARIANT[u.status]}>
                        {USER_STATUS_LABELS[u.status]}
                      </Badge>
                    </TableCell>
                    {isSuperAdmin && (
                      <TableCell className="text-right">
                        {u.id !== me?.id && (u.status === "ACTIVE" || u.status === "SUSPENDED") && (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={updateStatus.isPending}
                            onClick={() => toggleStatus(u.id, u.status)}
                          >
                            {u.status === "ACTIVE" ? "Suspendă" : "Reactivează"}
                          </Button>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
