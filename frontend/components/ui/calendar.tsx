"use client"

import { ChevronLeft, ChevronRight } from "lucide-react"
import { DayPicker } from "react-day-picker"
import { ro } from "react-day-picker/locale"

import { cn } from "@/lib/utils"

/**
 * react-day-picker styled with the app's own theme tokens rather than the
 * library stylesheet, so it follows light/dark like everything else.
 * Romanian locale, weeks starting Monday.
 */
function Calendar({
  className,
  classNames,
  showOutsideDays = true,
  ...props
}: React.ComponentProps<typeof DayPicker>) {
  return (
    <DayPicker
      locale={ro}
      weekStartsOn={1}
      showOutsideDays={showOutsideDays}
      className={cn("w-fit", className)}
      classNames={{
        months: "flex flex-col gap-5 sm:flex-row sm:gap-6",
        month: "flex flex-col gap-3",
        month_caption: "flex h-8 items-center justify-center",
        caption_label: "text-sm font-medium capitalize",
        nav: "flex items-center justify-between absolute inset-x-0 top-0 h-8",
        button_previous:
          "inline-flex size-8 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:pointer-events-none disabled:opacity-40",
        button_next:
          "inline-flex size-8 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:pointer-events-none disabled:opacity-40",
        month_grid: "w-full border-collapse",
        weekdays: "flex",
        weekday:
          "w-9 text-[0.7rem] font-medium text-muted-foreground capitalize",
        week: "flex w-full mt-1",
        day: "relative size-9 p-0 text-center text-sm focus-within:relative focus-within:z-20",
        day_button:
          "inline-flex size-9 items-center justify-center rounded-lg font-normal transition-colors hover:bg-muted aria-selected:opacity-100",
        // A range reads as one continuous band: square middle, rounded ends.
        range_start:
          "rounded-l-lg bg-accent [&>button]:bg-primary [&>button]:text-primary-foreground [&>button]:hover:bg-primary",
        range_end:
          "rounded-r-lg bg-accent [&>button]:bg-primary [&>button]:text-primary-foreground [&>button]:hover:bg-primary",
        range_middle:
          "bg-accent [&>button]:bg-transparent [&>button]:text-accent-foreground [&>button]:hover:bg-accent/70",
        selected: "",
        today: "font-semibold text-primary",
        outside: "text-muted-foreground/50",
        disabled: "text-muted-foreground/40 line-through",
        hidden: "invisible",
        ...classNames,
      }}
      components={{
        Chevron: ({ orientation, ...chevronProps }) =>
          orientation === "left" ? (
            <ChevronLeft className="size-4" {...chevronProps} />
          ) : (
            <ChevronRight className="size-4" {...chevronProps} />
          ),
      }}
      {...props}
    />
  )
}

export { Calendar }
