import type { Metadata } from "next"
import { SiteHeader } from "@/components/marketing/site-header"
import { SiteFooter } from "@/components/marketing/site-footer"

export const metadata: Metadata = {
  title: "Politica de confidențialitate",
  description: "Cum colectează, folosește și protejează BH Group datele tale personale.",
}

export default function PrivacyPolicyPage() {
  return (
    <div className="flex flex-1 flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16 sm:px-10">
        <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
          Politica de confidențialitate
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Ultima actualizare: 10 iulie 2026
        </p>

        <div className="mt-10 flex flex-col gap-8 text-sm leading-relaxed text-muted-foreground [&_h2]:mb-2 [&_h2]:mt-6 [&_h2]:text-base [&_h2]:font-semibold [&_h2]:text-foreground [&_p]:mb-3">
          <section>
            <h2>1. Cine suntem</h2>
            <p>
              BH Group administrează proprietăți pentru închiriere pe termen scurt și
              operează acest site pentru a permite oaspeților să caute și să rezerve
              cazare, respectiv proprietarilor să solicite servicii de administrare.
              Suntem operator de date cu caracter personal conform Regulamentului (UE)
              2016/679 (GDPR).
            </p>
          </section>

          <section>
            <h2>2. Ce date colectăm</h2>
            <p>
              Colectăm datele pe care ni le furnizezi direct: nume, adresă de email,
              telefon, detalii despre rezervare (perioadă, număr de oaspeți) sau despre
              proprietatea pe care dorești să o listezi. De asemenea, colectăm date
              tehnice minime (adresă IP, tip de browser) în scop de securitate, prin
              intermediul jurnalului de audit al platformei.
            </p>
          </section>

          <section>
            <h2>3. De ce folosim datele tale</h2>
            <p>
              Folosim datele pentru a procesa cererile de rezervare, a te contacta în
              legătură cu o proprietate, a preveni fraude și abuzuri, și pentru a
              respecta obligații legale (ex. evidența contractelor de cazare). Nu
              vindem datele tale către terți.
            </p>
          </section>

          <section>
            <h2>4. Cât timp păstrăm datele</h2>
            <p>
              Păstrăm datele de rezervare și cele fiscale conform termenelor legale de
              arhivare din România. Lead-urile necontactate pot fi șterse la cerere în
              orice moment.
            </p>
          </section>

          <section>
            <h2>5. Drepturile tale</h2>
            <p>
              Ai dreptul de acces, rectificare, ștergere, restricționare a prelucrării,
              portabilitate a datelor și dreptul de a te opune prelucrării. Pentru a-ți
              exercita oricare dintre aceste drepturi, ne poți contacta la adresa de
              email de mai jos.
            </p>
          </section>

          <section>
            <h2>6. Cookie-uri</h2>
            <p>
              Site-ul folosește cookie-uri strict necesare pentru funcționarea
              platformei (autentificare, preferință temă) și, opțional, cookie-uri de
              analiză, doar cu acordul tău exprimat prin bannerul afișat la prima
              vizită. Îți poți retrage oricând consimțământul din setările browserului.
            </p>
          </section>

          <section>
            <h2>7. Contact</h2>
            <p>
              Pentru orice întrebare legată de protecția datelor, ne poți scrie la{" "}
              <a href="mailto:contact@bhgroup.io" className="text-foreground underline">
                contact@bhgroup.io
              </a>
              .
            </p>
          </section>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
