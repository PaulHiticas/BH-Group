import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  images: {
    // The upload host (UPLOAD_PUBLIC_BASE_URL, e.g. http://localhost:8080/uploads)
    // is only reachable from the browser, not from the Next.js server itself
    // (inside the frontend container "localhost" means the frontend container).
    // Server-side image optimization would try to fetch it and fail, rendering
    // a blank/black image. Disabling optimization makes next/image render a
    // plain <img>, so the browser fetches it directly — same as before.
    unoptimized: true,
  },
};

export default nextConfig;
