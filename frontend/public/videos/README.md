# Cinematic background videos

This directory is where approved hero/background video clips go. None exist
yet — `CinematicBackgroundVideo` (see
`frontend/components/marketing/cinematic-background-video.tsx`) currently
runs with `srcMp4`/`srcWebm` omitted everywhere, so it only renders the
existing poster images. Drop in real, rights-cleared clips and pass their
paths as `srcMp4`/`srcWebm` to start playing video with zero other code
changes.

Needed, in priority order:

1. `home-hero.mp4` / `home-hero.webm` — homepage hero
   (`frontend/components/marketing/hero-section.tsx`). Premium apartment
   interior, slow camera movement, city/architecture, hospitality
   atmosphere. ~5-10s, natural loop, a few MB (not tens of MB).
2. `owner-management.mp4` / `owner-management.webm` — pentru-proprietari
   hero (`frontend/app/pentru-proprietari/page.tsx`). Housekeeping,
   property inspection, check-in prep. Should read as clearly distinct
   from the homepage clip, not a repeat.
3. `login.mp4` / `login.webm` — auth layout
   (`frontend/app/(auth)/layout.tsx`). Optional; same interior/property
   direction as the above, desktop-focused (mobile never mounts video,
   see `mobileFallback`).

Do not add stock footage from Airbnb, Booking.com, Guesty, Hostaway, hotel
brands, or agencies without clear usage rights.
