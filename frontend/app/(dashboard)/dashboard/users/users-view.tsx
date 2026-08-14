"use client"

import { useState } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { KeyRound, Mail, Plus, ShieldCheck, ShieldOff, UserPlus } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
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
import {
  useCreateUser,
  useResendInvite,
  useResetUserMfa,
  useUpdateUserStatus,
  useUsers,
} from "@/hooks/use-users"
import { useCurrentUser } from "@/hooks/use-current-user"
import { ALL_ROLES, ROLE_LABELS, USER_STATUS_BADGE_VARIANT, USER_STATUS_LABELS } from "@/lib/roles"
import type { Role, UserResponse, UserStatus } from "@/lib/api/types"

const createUserSchema = z.object({
  firstName: z.string().min(1, "Prenumele este obligatoriu").max(100),
  lastName: z.string().min(1, "Numele este obligatoriu").max(100),
  email: z.string().min(1, "Emailul este obligatoriu").email("Adresă de email invalidă"),
  phone: z.string().max(30).optional(),
  role: z.enum(ALL_ROLES as [Role, ...Role[]]),
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
          <DialogTitle>Cont nou de echipă</DialogTitle>
          <DialogDescription>
            Colegul primește un email de invitație și își alege singur parola pentru a-și activa
            contul — tu nu vezi și nu transmiți nicio parolă. 2FA îl activează tot el, din contul
            lui, din Setări.
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
              {createUser.isPending ? "Se trimite..." : "Trimite invitația"}
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}

function DisableUserDialog({ user }: { user: UserResponse }) {
  const [open, setOpen] = useState(false)
  const [typedEmail, setTypedEmail] = useState("")
  const updateStatus = useUpdateUserStatus()

  function handleConfirm() {
    updateStatus.mutate(
      { id: user.id, status: "DISABLED", confirmEmail: typedEmail },
      { onSuccess: () => { setOpen(false); setTypedEmail("") } }
    )
  }

  return (
    <AlertDialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) setTypedEmail("") }}>
      <AlertDialogTrigger render={<Button variant="destructive" size="sm" />}>
        Dezactivează definitiv
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Dezactivezi definitiv acest cont?</AlertDialogTitle>
          <AlertDialogDescription>
            {user.firstName} {user.lastName} nu se va mai putea autentifica. Acțiunea este
            greu de anulat. Scrie adresa de email a contului pentru a confirma:{" "}
            <strong>{user.email}</strong>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <Input
          value={typedEmail}
          onChange={(e) => setTypedEmail(e.target.value)}
          placeholder={user.email}
          autoComplete="off"
        />
        <AlertDialogFooter>
          <AlertDialogCancel>Anulează</AlertDialogCancel>
          <AlertDialogAction
            disabled={typedEmail.trim().toLowerCase() !== user.email.toLowerCase() || updateStatus.isPending}
            onClick={handleConfirm}
          >
            Dezactivează definitiv
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

export function UsersView() {
  const { data: me } = useCurrentUser()
  const [page, setPage] = useState(0)
  const { data, isLoading } = useUsers({ page })
  const updateStatus = useUpdateUserStatus()
  const resetMfa = useResetUserMfa()

  const resendInvite = useResendInvite()
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
                        <div className="flex justify-end gap-2">
                          {u.status === "PENDING" && (
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={resendInvite.isPending}
                              onClick={() => resendInvite.mutate(u.id)}
                            >
                              <Mail className="size-3.5" />
                              Retrimite invitația
                            </Button>
                          )}
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
                          {u.mfaEnabled && (
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={resetMfa.isPending}
                              onClick={() => resetMfa.mutate(u.id)}
                            >
                              Resetează 2FA
                            </Button>
                          )}
                          {u.id !== me?.id && u.status !== "DISABLED" && (
                            <DisableUserDialog user={u} />
                          )}
                        </div>
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
