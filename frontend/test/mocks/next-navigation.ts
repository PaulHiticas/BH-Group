import { vi } from "vitest"

/**
 * Shared next/navigation mock, registered globally via test/setup.ts.
 * Import `mockRouter`/`mockSearchParams` from a test file to assert
 * navigation calls or to change the current search params/pathname for
 * that test (mockPathname.value = "/somewhere").
 */
export const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
  back: vi.fn(),
  forward: vi.fn(),
  refresh: vi.fn(),
  prefetch: vi.fn(),
}

export const mockPathname = { value: "/" }
export const mockSearchParams = { value: new URLSearchParams() }

vi.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
  usePathname: () => mockPathname.value,
  useSearchParams: () => mockSearchParams.value,
  useParams: () => ({}),
}))
