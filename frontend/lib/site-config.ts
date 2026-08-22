/**
 * Single source of truth for public-facing company contact details.
 * Deliberately no hardcoded fallback phone/email - an unset value means
 * the UI must offer the contact form instead of inventing a number.
 */
export const siteConfig = {
  companyEmail: process.env.NEXT_PUBLIC_COMPANY_EMAIL || null,
  companyPhone: process.env.NEXT_PUBLIC_COMPANY_PHONE || null,
}
