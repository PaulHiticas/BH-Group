import type { Metadata } from "next"
import { SiteHeader } from "@/components/marketing/site-header"
import { ServicesSection } from "@/components/marketing/services-section"
import { ProcessSection } from "@/components/marketing/process-section"
import { RevenueEstimateSection } from "@/components/marketing/revenue-estimate-section"
import { FaqSection } from "@/components/marketing/faq-section"
import { LeadFormSection } from "@/components/marketing/lead-form-section"
import { SiteFooter } from "@/components/marketing/site-footer"
import { ChatWidget } from "@/components/marketing/chat-widget"
import { SiteBackgroundVideo } from "@/components/marketing/site-background-video"

export const metadata: Metadata = {
  title: "Listează-ți proprietatea",
  description:
    "Administrare premium pentru proprietatea ta — curățenie, comunicare cu oaspeții și rapoarte financiare transparente.",
}

export default function PentruProprietariPage() {
  return (
    <div className="flex flex-1 flex-col overflow-x-hidden">
      <SiteBackgroundVideo />
      <SiteHeader />

      <section className="relative flex min-h-[60vh] items-end overflow-hidden text-white">
        {/* No local video here anymore - the page-wide <SiteBackgroundVideo />
            above shows through this transparent section; this gradient adds
            the extra darkening the white heading/text need on top of it. */}
        <div className="absolute inset-0 bg-gradient-to-t from-black via-black/60 to-black/20" />
        <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 pb-16 pt-40 sm:px-10">
          <span className="w-fit rounded-full border border-white/20 bg-white/10 px-4 py-1.5 text-xs font-medium tracking-wide text-white/80 backdrop-blur-sm">
            Pentru proprietari
          </span>
          <h1 className="max-w-2xl text-balance font-heading text-4xl font-semibold leading-[1.1] tracking-tight sm:text-5xl lg:text-6xl">
            Proprietatea ta, administrată ca un hotel de 5 stele.
          </h1>
          <p className="max-w-xl text-balance text-lg text-white/75">
            Curățenie, prețuri adaptate sezonului, comunicare cu oaspeții și rapoarte financiare —
            ne ocupăm de tot, tu încasezi venitul.
          </p>
        </div>
      </section>

      <div id="servicii">
        <ServicesSection />
      </div>
      <ProcessSection />
      <RevenueEstimateSection />
      <div id="faq">
        <FaqSection />
      </div>
      <div id="formular">
        <LeadFormSection />
      </div>
      <SiteFooter />
      <ChatWidget />
    </div>
  )
}
