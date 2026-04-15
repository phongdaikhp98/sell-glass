"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  getProductImages,
  uploadProductImage,
  deleteProductImage,
  setPrimaryImage,
  reorderImages,
  type ProductImage,
} from "@/lib/product-image.api";

interface Props {
  productId: string;
  productName: string;
}

export default function ProductImageManager({ productId, productName }: Props) {
  const [images, setImages] = useState<ProductImage[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [settingPrimaryId, setSettingPrimaryId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getProductImages(productId);
      setImages(data.sort((a, b) => a.sortOrder - b.sortOrder));
    } catch {
      toast.error("Không thể tải ảnh sản phẩm");
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (!files.length) return;
    setUploading(true);
    try {
      for (const file of files) {
        await uploadProductImage(productId, file);
      }
      toast.success(`Đã tải lên ${files.length} ảnh`);
      await load();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Tải ảnh thất bại");
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  async function handleDelete(image: ProductImage) {
    setDeletingId(image.id);
    try {
      await deleteProductImage(productId, image.id);
      toast.success("Đã xóa ảnh");
      await load();
    } catch {
      toast.error("Không thể xóa ảnh");
    } finally {
      setDeletingId(null);
    }
  }

  async function handleSetPrimary(image: ProductImage) {
    if (image.isPrimary) return;
    setSettingPrimaryId(image.id);
    try {
      await setPrimaryImage(productId, image.id);
      setImages((prev) =>
        prev.map((img) => ({ ...img, isPrimary: img.id === image.id }))
      );
      toast.success("Đã đặt ảnh đại diện");
    } catch {
      toast.error("Không thể đặt ảnh đại diện");
    } finally {
      setSettingPrimaryId(null);
    }
  }

  async function moveImage(index: number, direction: "up" | "down") {
    const newImages = [...images];
    const swapIndex = direction === "up" ? index - 1 : index + 1;
    if (swapIndex < 0 || swapIndex >= newImages.length) return;
    [newImages[index], newImages[swapIndex]] = [newImages[swapIndex], newImages[index]];
    const reordered = newImages.map((img, i) => ({ ...img, sortOrder: i }));
    setImages(reordered);
    try {
      await reorderImages(
        productId,
        reordered.map((img) => ({ id: img.id, sortOrder: img.sortOrder }))
      );
    } catch {
      toast.error("Không thể lưu thứ tự ảnh");
      await load();
    }
  }

  if (loading) {
    return (
      <div className="grid grid-cols-3 gap-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="aspect-square animate-pulse rounded-lg bg-muted" />
        ))}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {images.length} ảnh — {productName}
        </p>
        <div>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            className="hidden"
            onChange={handleFileChange}
          />
          <Button
            size="sm"
            disabled={uploading}
            onClick={() => fileInputRef.current?.click()}
          >
            {uploading ? "Đang tải lên..." : "Thêm ảnh"}
          </Button>
        </div>
      </div>

      {images.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-muted-foreground/30 py-12 text-center cursor-pointer hover:border-muted-foreground/50 transition-colors"
          onClick={() => fileInputRef.current?.click()}
        >
          <p className="text-sm text-muted-foreground">Chưa có ảnh nào</p>
          <p className="text-xs text-muted-foreground mt-1">Click để tải ảnh lên</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {images.map((image, index) => (
            <div
              key={image.id}
              className={`group relative rounded-lg border-2 overflow-hidden ${
                image.isPrimary ? "border-primary" : "border-transparent"
              }`}
            >
              {/* Image */}
              <div className="aspect-square bg-muted">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={image.url}
                  alt={`Ảnh ${index + 1}`}
                  className="h-full w-full object-cover"
                />
              </div>

              {/* Primary badge */}
              {image.isPrimary && (
                <div className="absolute left-1 top-1 rounded bg-primary px-1.5 py-0.5 text-[10px] font-medium text-primary-foreground">
                  Đại diện
                </div>
              )}

              {/* Actions overlay */}
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-black/60 opacity-0 transition-opacity group-hover:opacity-100">
                {!image.isPrimary && (
                  <Button
                    size="sm"
                    variant="secondary"
                    className="h-7 text-xs w-24"
                    disabled={settingPrimaryId === image.id}
                    onClick={() => handleSetPrimary(image)}
                  >
                    {settingPrimaryId === image.id ? "..." : "Đặt đại diện"}
                  </Button>
                )}

                <div className="flex gap-1">
                  <Button
                    size="sm"
                    variant="secondary"
                    className="h-7 w-8 p-0 text-xs"
                    disabled={index === 0}
                    onClick={() => moveImage(index, "up")}
                    title="Lên"
                  >
                    ↑
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    className="h-7 w-8 p-0 text-xs"
                    disabled={index === images.length - 1}
                    onClick={() => moveImage(index, "down")}
                    title="Xuống"
                  >
                    ↓
                  </Button>
                </div>

                <Button
                  size="sm"
                  variant="destructive"
                  className="h-7 text-xs w-24"
                  disabled={deletingId === image.id}
                  onClick={() => handleDelete(image)}
                >
                  {deletingId === image.id ? "Đang xóa..." : "Xóa ảnh"}
                </Button>
              </div>
            </div>
          ))}

          {/* Upload tile */}
          <div
            className="aspect-square flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-muted-foreground/30 text-muted-foreground transition-colors hover:border-muted-foreground/50"
            onClick={() => fileInputRef.current?.click()}
          >
            <span className="text-2xl">+</span>
            <span className="text-xs mt-1">Thêm ảnh</span>
          </div>
        </div>
      )}

      <p className="text-xs text-muted-foreground">
        Hỗ trợ JPEG, PNG, WebP · Tối đa 10 MB mỗi ảnh · Hover để chỉnh sửa
      </p>
    </div>
  );
}
