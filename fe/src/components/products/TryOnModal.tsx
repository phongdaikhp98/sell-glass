"use client";

import { useEffect, useRef, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

type Status = "idle" | "loading" | "running" | "error";

interface TryOnModalProps {
  open: boolean;
  onClose: () => void;
  glassesImageUrl: string;
  productName: string;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type FaceLandmarkerInstance = any;

export default function TryOnModal({
  open,
  onClose,
  glassesImageUrl,
  productName,
}: TryOnModalProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rafRef = useRef<number>(0);
  const landmarkerRef = useRef<FaceLandmarkerInstance>(null);
  const glassesImgRef = useRef<HTMLImageElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [status, setStatus] = useState<Status>("idle");

  useEffect(() => {
    if (!open) {
      cancelAnimationFrame(rafRef.current);
      streamRef.current?.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
      landmarkerRef.current?.close();
      landmarkerRef.current = null;
      setStatus("idle");
      return;
    }

    let cancelled = false;
    setStatus("loading");

    async function init() {
      try {
        const { FaceLandmarker, FilesetResolver } = await import(
          "@mediapipe/tasks-vision"
        );
        if (cancelled) return;

        // WASM và model được self-host trong /public/mediapipe-wasm/ và /public/mediapipe-models/
        // để tránh phụ thuộc CDN ngoài (bảo mật + CSP)
        const vision = await FilesetResolver.forVisionTasks("/mediapipe-wasm");
        if (cancelled) return;

        const fl = await FaceLandmarker.createFromOptions(vision, {
          baseOptions: {
            modelAssetPath: "/mediapipe-models/face_landmarker.task",
          },
          runningMode: "VIDEO",
          numFaces: 1,
        });
        if (cancelled) {
          fl.close();
          return;
        }
        landmarkerRef.current = fl;

        // Preload glasses image
        const img = new Image();
        img.crossOrigin = "anonymous";
        img.src = glassesImageUrl;
        await new Promise<void>((resolve) => {
          img.onload = () => resolve();
          img.onerror = () => resolve();
        });
        glassesImgRef.current = img;
        if (cancelled) return;

        // Start webcam
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { width: 640, height: 480, facingMode: "user" },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;

        const video = videoRef.current!;
        video.srcObject = stream;
        await video.play();

        // Size canvas to video
        const canvas = canvasRef.current!;
        canvas.width = video.videoWidth || 640;
        canvas.height = video.videoHeight || 480;

        setStatus("running");
        startLoop(fl, video, canvas);
      } catch {
        if (!cancelled) setStatus("error");
      }
    }

    init();

    return () => {
      cancelled = true;
      cancelAnimationFrame(rafRef.current);
      streamRef.current?.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
      landmarkerRef.current?.close();
      landmarkerRef.current = null;
    };
  }, [open, glassesImageUrl]);

  function startLoop(
    fl: FaceLandmarkerInstance,
    video: HTMLVideoElement,
    canvas: HTMLCanvasElement
  ) {
    function tick() {
      rafRef.current = requestAnimationFrame(tick);
      if (video.readyState < 2) return;

      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      // Update canvas size if changed
      if (canvas.width !== video.videoWidth && video.videoWidth > 0) {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
      }

      // Draw mirrored webcam frame
      ctx.save();
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      ctx.restore();

      // Detect face
      const results = fl.detectForVideo(video, performance.now());
      if (!results.faceLandmarks?.length) return;

      const lm = results.faceLandmarks[0];
      const leftEye = lm[33];   // person's left eye outer corner
      const rightEye = lm[263]; // person's right eye outer corner
      if (!leftEye || !rightEye) return;

      // Mirror X to match the flipped canvas
      const lx = (1 - leftEye.x) * canvas.width;
      const rx = (1 - rightEye.x) * canvas.width;
      const ly = leftEye.y * canvas.height;
      const ry = rightEye.y * canvas.height;

      const midX = (lx + rx) / 2;
      const midY = (ly + ry) / 2;
      const angle = Math.atan2(ry - ly, rx - lx);
      const eyeSpan = Math.hypot(rx - lx, ry - ly);
      const glassesW = eyeSpan * 2.8;
      const img = glassesImgRef.current;
      const glassesH =
        img?.naturalWidth
          ? (glassesW * img.naturalHeight) / img.naturalWidth
          : glassesW * 0.4;

      ctx.save();
      ctx.translate(midX, midY);
      ctx.rotate(angle);
      ctx.globalCompositeOperation = "multiply";
      if (img) ctx.drawImage(img, -glassesW / 2, -glassesH / 2, glassesW, glassesH);
      ctx.restore();
    }
    tick();
  }

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) onClose(); }}>
      <DialogContent className="sm:max-w-2xl" showCloseButton>
        <DialogHeader>
          <DialogTitle>Thử kính ảo — {productName}</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col items-center gap-3">
          {/* Hidden video element as webcam source */}
          <video ref={videoRef} className="hidden" playsInline muted />

          {/* Canvas display area */}
          <div className="relative w-full overflow-hidden rounded-lg bg-black">
            {status === "loading" && (
              <div className="flex aspect-video w-full items-center justify-center text-sm text-white/70">
                Đang tải model nhận diện khuôn mặt...
              </div>
            )}
            {status === "error" && (
              <div className="flex aspect-video w-full items-center justify-center px-4 text-center text-sm text-red-400">
                Không thể truy cập webcam. Vui lòng kiểm tra quyền truy cập camera trên trình duyệt.
              </div>
            )}
            <canvas
              ref={canvasRef}
              className={`w-full ${status === "running" ? "block" : "hidden"}`}
            />
          </div>

          {status === "running" && (
            <p className="text-center text-xs text-muted-foreground">
              💡 Hướng mặt thẳng vào camera, giữ khoảng cách 40–60 cm để kết quả tốt nhất
            </p>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
