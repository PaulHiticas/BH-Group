/**
 * Champagne-gold "hospitality" background decor for public pages: a few
 * low-alpha gold glows, a fine static grain, and a very faint contour.
 * Tuned separately for light and dark (see .site-bg in globals.css) so
 * both themes look intentional, not just plain black/white. Purely
 * decorative - fixed behind everything, no pointer events, no animation.
 */
export function SiteBackground() {
  return (
    <div aria-hidden className="site-bg pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <svg
        className="site-bg-contour absolute inset-0 h-full w-full"
        viewBox="0 0 1440 900"
        preserveAspectRatio="none"
        fill="none"
      >
        <path d="M -100 640 C 260 540, 680 740, 1180 580 S 1720 480, 1900 600" />
        <path d="M -150 160 C 260 260, 640 40, 1060 200 S 1660 340, 1900 210" />
        <path d="M -150 420 C 220 360, 560 480, 940 400 S 1600 300, 1900 420" />
      </svg>
    </div>
  )
}
