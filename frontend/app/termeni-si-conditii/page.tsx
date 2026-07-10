import type { Metadata } from "next"
import { SiteHeader } from "@/components/marketing/site-header"
import { SiteFooter } from "@/components/marketing/site-footer"

export const metadata: Metadata = {
  title: "Termeni și condiții",
  description: "Termenii și condițiile de utilizare a platformei BH Group și de rezervare a proprietăților.",
}

export default function TermsPage() {
  return (
    <div className="flex flex-1 flex-col">
      <SiteHeader />
      <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16 sm:px-10">
        <h1 className="font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
          Termeni și condiții
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Ultima actualizare: 10 iulie 2026
        </p>

        <div className="mt-10 flex flex-col gap-8 text-sm leading-relaxed text-muted-foreground [&_h2]:mb-2 [&_h2]:mt-6 [&_h2]:text-base [&_h2]:font-semibold [&_h2]:text-foreground [&_p]:mb-3">
          <section>
            <h2>1. Obiectul contractului</h2>
            <p>
              Acești termeni guvernează utilizarea site-ului BH Group și procesul de
              rezervare a proprietăților listate pe platformă. Prin trimiterea unei
              cereri de rezervare sau a unui formular de contact, confirmi că ai citit
              și ești de acord cu acești termeni.
            </p>
          </section>

          <section>
            <h2>2. Rezervări</h2>
            <p>
              O cerere de rezervare transmisă prin site nu constituie o confirmare
              automată — rezervarea devine fermă doar după confirmarea din partea
              echipei BH Group, comunicată pe emailul furnizat. Prețul afișat este
              orientativ și poate varia în funcție de sezon și durata sejurului.
            </p>
          </section>

          <section>
            <h2>3. Anulări și modificări</h2>
            <p>
              Poți gestiona, modifica sau anula o rezervare folosind link-ul unic
              primit prin email după confirmare. Condițiile specifice de anulare
              (termene, eventuale penalizări) sunt comunicate individual pentru fiecare
              proprietate, în funcție de politica proprietarului.
            </p>
          </section>

          <section>
            <h2>4. Obligațiile oaspeților</h2>
            <p>
              Oaspeții sunt responsabili pentru respectarea regulilor proprietății,
              a orelor de check-in/check-out și pentru orice daune produse în timpul
              sejurului. Numărul de oaspeți nu poate depăși capacitatea maximă afișată
              pentru proprietate.
            </p>
          </section>

          <section>
            <h2>5. Proprietari și lead-uri</h2>
            <p>
              Prin trimiterea formularului „Listează-ți proprietatea”, ne oferi acordul
              de a te contacta telefonic sau prin email în legătură cu serviciile
              noastre de administrare. Nu suntem obligați să acceptăm listarea oricărei
              proprietăți.
            </p>
          </section>

          <section>
            <h2>6. Limitarea răspunderii</h2>
            <p>
              BH Group depune eforturi rezonabile pentru a menține informațiile de pe
              site actualizate, dar nu garantează disponibilitatea neîntreruptă a
              platformei sau acuratețea absolută a fiecărei liste de proprietăți.
            </p>
          </section>

          <section>
            <h2>7. Contact</h2>
            <p>
              Pentru întrebări legate de acești termeni, ne poți scrie la{" "}
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
