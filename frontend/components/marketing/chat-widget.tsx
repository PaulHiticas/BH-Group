"use client"

import { useEffect, useRef, useState } from "react"
import { motion, AnimatePresence } from "motion/react"
import { MessageCircle, Send, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import { siteConfig } from "@/lib/site-config"
import { useAssistantChat } from "@/hooks/use-assistant-chat"
import type { AssistantMessage } from "@/lib/api/assistant"

interface ChatMessage {
  id: number
  role: "bot" | "user"
  text: string
}

const CONTACT_FALLBACK =
  siteConfig.companyEmail || siteConfig.companyPhone
    ? [siteConfig.companyEmail, siteConfig.companyPhone].filter(Boolean).join(" sau la ")
    : "prin formularul de contact de pe pagina „Pentru proprietari”"

const ERROR_FALLBACK = `Nu am putut trimite mesajul chiar acum. Te rugăm să ne contactezi ${CONTACT_FALLBACK}.`

const GREETING = "Salut! Sunt asistentul virtual BH Group. Întreabă-mă despre check-in, anulare, comisioane sau cum îți poți lista proprietatea."

let messageId = 0

export function ChatWidget() {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState("")
  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: messageId++, role: "bot", text: GREETING },
  ])
  const scrollRef = useRef<HTMLDivElement>(null)
  const chatMutation = useAssistantChat()

  useEffect(() => {
    const el = scrollRef.current
    if (typeof el?.scrollTo === "function") {
      el.scrollTo({ top: el.scrollHeight, behavior: "smooth" })
    }
  }, [messages, chatMutation.isPending])

  async function handleSend() {
    const question = input.trim()
    if (!question || chatMutation.isPending) return

    const userMessage: ChatMessage = { id: messageId++, role: "user", text: question }
    // The very first message is a canned local greeting, never a real turn -
    // the Anthropic API requires the conversation to start with a "user"
    // message, so it's excluded from what gets sent.
    const conversation = [...messages, userMessage].slice(1)
    const history: AssistantMessage[] = conversation.map((message) => ({
      role: message.role === "bot" ? "assistant" : "user",
      content: message.text,
    }))

    setMessages((prev) => [...prev, userMessage])
    setInput("")

    try {
      const response = await chatMutation.mutateAsync(history)
      setMessages((prev) => [...prev, { id: messageId++, role: "bot", text: response.message }])
    } catch {
      setMessages((prev) => [...prev, { id: messageId++, role: "bot", text: ERROR_FALLBACK }])
    }
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
              {chatMutation.isPending && (
                <div className="flex max-w-[85%] items-center gap-1 self-start rounded-2xl bg-muted px-3.5 py-2.5">
                  {[0, 1, 2].map((i) => (
                    <span
                      key={i}
                      className="size-1.5 animate-bounce rounded-full bg-muted-foreground/60"
                      style={{ animationDelay: `${i * 0.15}s` }}
                    />
                  ))}
                </div>
              )}
            </div>

            <div className="flex items-center gap-2 border-t border-border/60 p-3">
              <Input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleSend()
                }}
                placeholder="Scrie o întrebare..."
                disabled={chatMutation.isPending}
                className="flex-1"
              />
              <Button
                size="icon"
                onClick={handleSend}
                disabled={!input.trim() || chatMutation.isPending}
                aria-label="Trimite mesaj"
              >
                <Send className="size-4" />
              </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  )
}
