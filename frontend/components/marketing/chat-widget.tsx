"use client"

import { useEffect, useRef, useState } from "react"
import { motion, AnimatePresence } from "motion/react"
import { MessageCircle, Send, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import { siteConfig } from "@/lib/site-config"

interface ChatMessage {
  id: number
  role: "bot" | "user"
  text: string
}

interface KnowledgeEntry {
  keywords: string[]
  answer: string
}

const CONTACT_FALLBACK =
  siteConfig.companyEmail || siteConfig.companyPhone
    ? [siteConfig.companyEmail, siteConfig.companyPhone].filter(Boolean).join(" sau la ")
    : "prin formularul de contact de pe pagina „Pentru proprietari”"

const KNOWLEDGE_BASE: KnowledgeEntry[] = [
  {
    keywords: ["check-in", "checkin", "check in", "ora"],
    answer: "Ora de check-in și check-out variază pe proprietate — o vezi clar pe pagina fiecărui anunț, înainte de a trimite cererea. Unele proprietăți au check-in autonom cu smart lock; altele au check-in cu personal.",
  },
  {
    keywords: ["anulare", "anulez", "cancel", "retur"],
    answer: "Fiecare proprietate are propria politică de anulare (flexibilă, moderată, strictă sau nerambursabilă) — o vezi afișată clar pe pagina anunțului, înainte de a trimite cererea.",
  },
  {
    keywords: ["comision", "cost", "pret", "preț", "cât cost", "cat costa"],
    answer: "Comisionul de administrare depinde de locație, tipul proprietății și serviciile incluse — stabilim totul transparent la prima discuție, fără costuri ascunse.",
  },
  {
    keywords: ["curatenie", "curățenie", "lenjerie"],
    answer: "Fiecare sejur include curățenie și lenjerie/prosoape curate; taxa de curățenie (dacă se aplică) apare defalcat, separat, în cererea de rezervare.",
  },
  {
    keywords: ["plata", "plată", "card", "bani"],
    answer: "Nu se percepe nicio plată când trimiți cererea de rezervare — trimiți doar o cerere, pe care echipa o confirmă manual. Modalitatea de plată se stabilește cu echipa după confirmare.",
  },
  {
    keywords: ["animal", "caine", "câine", "pisica", "pisică", "pet"],
    answer: "Unele proprietăți acceptă animale de companie — poți filtra după această facilitate în pagina de căutare.",
  },
  {
    keywords: ["listez", "listare", "proprietar", "proprietatea mea", "administrare"],
    answer: "Perfect! Apasă pe \"Listează proprietatea ta\" din pagină, lasă-ne datele tale de contact, și revenim cu o estimare de venit.",
  },
  {
    keywords: ["platforme", "airbnb", "booking"],
    answer: "Publicăm proprietățile pe canalele de distribuție relevante și prin rezervare directă pe platforma noastră; disponibilitatea exactă pe fiecare canal diferă în funcție de proprietate.",
  },
  {
    keywords: ["contact", "telefon", "email", "sun"],
    answer: `Ne poți contacta ${CONTACT_FALLBACK}.`,
  },
  {
    keywords: ["rezerv", "book", "caut", "apartament"],
    answer: "Poți căuta și rezerva direct din pagina \"Vezi apartamente\" din meniu — alege orașul, datele și numărul de oaspeți.",
  },
]

const FALLBACK_ANSWER =
  `Nu sunt sigur că am înțeles corect. Poți reformula, sau ne poți contacta ${CONTACT_FALLBACK} — nu vreau să-ți dau un răspuns pe care nu-l pot garanta.`

function findAnswer(question: string): string {
  const normalized = question.toLowerCase()
  const match = KNOWLEDGE_BASE.find((entry) =>
    entry.keywords.some((keyword) => normalized.includes(keyword))
  )
  return match?.answer ?? FALLBACK_ANSWER
}

let messageId = 0

export function ChatWidget() {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState("")
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: messageId++,
      role: "bot",
      text: "Salut! Sunt asistentul virtual BH Group. Întreabă-mă despre check-in, anulare, comisioane sau cum îți poți lista proprietatea.",
    },
  ])
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" })
  }, [messages])

  function handleSend() {
    const question = input.trim()
    if (!question) return

    const userMessage: ChatMessage = { id: messageId++, role: "user", text: question }
    setMessages((prev) => [...prev, userMessage])
    setInput("")

    window.setTimeout(() => {
      const answer = findAnswer(question)
      setMessages((prev) => [...prev, { id: messageId++, role: "bot", text: answer }])
    }, 500)
  }

  return (
    <>
      <motion.button
        onClick={() => setOpen((v) => !v)}
        initial={{ scale: 0, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5, delay: 1, ease: [0.16, 1, 0.3, 1] }}
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
        className="fixed bottom-6 right-6 z-50 flex size-14 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg"
        aria-label="Deschide chat"
      >
        <AnimatePresence mode="wait" initial={false}>
          {open ? (
            <motion.span key="close" initial={{ rotate: -90, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: 90, opacity: 0 }}>
              <X className="size-6" />
            </motion.span>
          ) : (
            <motion.span key="open" initial={{ rotate: 90, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: -90, opacity: 0 }}>
              <MessageCircle className="size-6" />
            </motion.span>
          )}
        </AnimatePresence>
      </motion.button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
            className="fixed bottom-24 right-6 z-50 flex h-[28rem] w-[22rem] max-w-[calc(100vw-3rem)] flex-col overflow-hidden rounded-2xl border border-border/60 bg-card shadow-2xl"
          >
            <div className="flex items-center gap-2 border-b border-border/60 bg-primary px-4 py-3 text-primary-foreground">
              <MessageCircle className="size-4" />
              <p className="text-sm font-medium">Asistent BH Group</p>
            </div>

            <div ref={scrollRef} className="flex flex-1 flex-col gap-3 overflow-y-auto p-4">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={cn(
                    "max-w-[85%] rounded-2xl px-3.5 py-2 text-sm leading-relaxed",
                    message.role === "bot"
                      ? "self-start bg-muted text-foreground"
                      : "self-end bg-primary text-primary-foreground"
                  )}
                >
                  {message.text}
                </div>
              ))}
            </div>

            <div className="flex items-center gap-2 border-t border-border/60 p-3">
              <Input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleSend()
                }}
                placeholder="Scrie o întrebare..."
                className="flex-1"
              />
              <Button size="icon" onClick={handleSend} disabled={!input.trim()}>
                <Send className="size-4" />
              </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  )
}
