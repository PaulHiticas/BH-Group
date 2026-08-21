import { describe, expect, it, vi, beforeEach } from "vitest"
import { screen, renderWithProviders, userEvent } from "@/test/utils"
import { LoginForm } from "./login-form"
import { useLogin } from "@/hooks/use-auth"

vi.mock("@/hooks/use-auth", () => ({
  useLogin: vi.fn(),
}))

describe("LoginForm", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("shows validation errors and does not submit when the form is empty", async () => {
    const mutate = vi.fn()
    vi.mocked(useLogin).mockReturnValue({ mutate, isPending: false } as never)
    const user = userEvent.setup()
    renderWithProviders(<LoginForm />)

    await user.click(screen.getByRole("button", { name: "Autentificare" }))

    expect(await screen.findByText("Emailul este obligatoriu")).toBeInTheDocument()
    expect(screen.getByText("Parola este obligatorie")).toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()
  })

  it("rejects a malformed email without calling the login action", async () => {
    // "not-an-email" has no "@", so the native type="email" constraint
    // validation blocks the submit before React ever sees it (same as a
    // real browser, since the form has no noValidate) — assert on the
    // actually-observable outcome (no mutation) rather than on which
    // validation layer (native vs. zod) produced it.
    const mutate = vi.fn()
    vi.mocked(useLogin).mockReturnValue({ mutate, isPending: false } as never)
    const user = userEvent.setup()
    renderWithProviders(<LoginForm />)

    await user.type(screen.getByLabelText("Email"), "not-an-email")
    await user.type(screen.getByLabelText("Parolă"), "parola123")
    await user.click(screen.getByRole("button", { name: "Autentificare" }))

    expect(mutate).not.toHaveBeenCalled()
  })

  it("calls the login action with the entered email and password", async () => {
    const mutate = vi.fn()
    vi.mocked(useLogin).mockReturnValue({ mutate, isPending: false } as never)
    const user = userEvent.setup()
    renderWithProviders(<LoginForm />)

    await user.type(screen.getByLabelText("Email"), "admin@bhgroup.io")
    await user.type(screen.getByLabelText("Parolă"), "parola123")
    await user.click(screen.getByRole("button", { name: "Autentificare" }))

    expect(mutate).toHaveBeenCalledTimes(1)
    expect(mutate).toHaveBeenCalledWith({ email: "admin@bhgroup.io", password: "parola123" })
  })

  it("disables the submit button while the login mutation is pending", () => {
    vi.mocked(useLogin).mockReturnValue({ mutate: vi.fn(), isPending: true } as never)
    renderWithProviders(<LoginForm />)

    expect(screen.getByRole("button", { name: "Se autentifică..." })).toBeDisabled()
  })
})
