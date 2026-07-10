import type { MetadataRoute } from "next"

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://bhgroup.io"

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/dashboard", "/login", "/forgot-password", "/reset-password", "/mfa", "/manage-booking"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  }
}
