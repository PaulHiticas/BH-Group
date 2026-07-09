import { SiteHeader } from "@/components/marketing/site-header"
import { HeroSection } from "@/components/marketing/hero-section"
import { HomeSearchBar } from "@/components/marketing/home-search-bar"
import { WelcomeSection } from "@/components/marketing/welcome-section"
import { PropertiesShowcase } from "@/components/marketing/properties-showcase"
import { InfoSection } from "@/components/marketing/info-section"
import { TestimonialsSection } from "@/components/marketing/testimonials-section"
import { CtaSection } from "@/components/marketing/cta-section"
import { SiteFooter } from "@/components/marketing/site-footer"
import { ChatWidget } from "@/components/marketing/chat-widget"

export default function Home() {
  return (
    <div className="flex flex-1 flex-col overflow-x-hidden">
      <SiteHeader />
      <HeroSection />
      <HomeSearchBar />
      <WelcomeSection />
      <div id="proprietati">
        <PropertiesShowcase />
      </div>
      <TestimonialsSection />
      <div id="informatii">
        <InfoSection />
      </div>
      <CtaSection />
      <SiteFooter />
      <ChatWidget />
    </div>
  )
}
