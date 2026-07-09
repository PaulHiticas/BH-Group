"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useVerifyMfaLogin } from "@/hooks/use-auth"
import { useAuthStore } from "@/lib/stores/auth-store"

const schema = z.object({
  code: z.string().regex(/^\d{6}$/, "Codul trebuie să aibă 6 cifre"),
})

type FormValues = z.infer<typeof schema>

export function MfaVerifyForm() {
  const router = useRouter()
  const verifyMfaLogin = useVerifyMfaLogin()
  // Checked once on mount — a successful verification clears the challenge
  // token as part of navigating away, which must not re-trigger this guard.
  const [hadChallengeToken] = useState(() => !!useAuthStore.getState().mfaChallengeToken)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { code: "" },
  })

  useEffect(() => {
    if (!hadChallengeToken) {
      router.replace("/login")
    }
  }, [hadChallengeToken, router])

  function onSubmit(values: FormValues) {
    verifyMfaLogin.mutate(values.code)
  }

  return (
    <Card className="border-white/15 bg-background/95 shadow-2xl backdrop-blur-md">
      <CardHeader>
        <CardTitle className="text-xl">Verificare în doi pași</CardTitle>
        <CardDescription>
          Introdu codul din aplicația de autentificare (Google Authenticator, Authy).
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cod de verificare</FormLabel>
                  <FormControl>
                    <Input
                      inputMode="numeric"
                      maxLength={6}
                      placeholder="123456"
                      autoComplete="one-time-code"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button type="submit" className="w-full" disabled={verifyMfaLogin.isPending}>
              {verifyMfaLogin.isPending ? "Se verifică..." : "Confirmă"}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  )
}
