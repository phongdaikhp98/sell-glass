import type { NextConfig } from "next";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const securityHeaders = [
  // Chống clickjacking
  { key: "X-Frame-Options", value: "DENY" },
  // Chặn MIME type sniffing
  { key: "X-Content-Type-Options", value: "nosniff" },
  // Giới hạn Referer — quan trọng để bảo vệ token reset-password trong URL
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  // Cho phép camera (TryOn), chặn microphone và các API nhạy cảm khác
  { key: "Permissions-Policy", value: "camera=(self), microphone=()" },
  // HSTS — chỉ apply khi đã có HTTPS (production)
  {
    key: "Strict-Transport-Security",
    value: "max-age=63072000; includeSubDomains; preload",
  },
  // Content Security Policy
  {
    key: "Content-Security-Policy",
    value: [
      "default-src 'self'",
      // wasm-unsafe-eval cần cho MediaPipe WASM
      "script-src 'self' 'wasm-unsafe-eval'",
      // style-src: unsafe-inline do Tailwind inject style
      "style-src 'self' 'unsafe-inline'",
      // img: blob cho canvas capture, data cho base64, https cho Cloudinary CDN
      "img-src 'self' data: blob: https:",
      // media: blob cho canvas stream (TryOn webcam)
      "media-src 'self' blob:",
      // worker: blob cho MediaPipe Web Worker
      "worker-src 'self' blob:",
      // connect: API backend + CDN MediaPipe (WASM + model)
      `connect-src 'self' ${API_URL} https://cdn.jsdelivr.net https://storage.googleapis.com`,
      // frame: không cho iframe embed
      "frame-src 'none'",
      "frame-ancestors 'none'",
    ].join("; "),
  },
];

const nextConfig: NextConfig = {
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: securityHeaders,
      },
    ];
  },
};

export default nextConfig;
