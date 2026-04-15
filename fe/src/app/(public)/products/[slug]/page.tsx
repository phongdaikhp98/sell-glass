"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeftIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { getProductBySlug } from "@/lib/product.api";
import { useAuthStore } from "@/store/auth.store";
import api from "@/lib/api";
import type { Product, ProductVariant } from "@/types";

const formatVND = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    amount
  );

const genderLabel: Record<string, string> = {
  MEN: "Nam",
  WOMEN: "Nữ",
  UNISEX: "Unisex",
};

export default function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();

  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [activeImage, setActiveImage] = useState<string | null>(null);
  const [selectedColor, setSelectedColor] = useState<string | null>(null);
  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [addingToCart, setAddingToCart] = useState(false);

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    getProductBySlug(slug)
      .then((data) => {
        setProduct(data);
        const primary =
          data.images.find((img) => img.isPrimary)?.url ??
          data.images[0]?.url ??
          data.primaryImageUrl ??
          null;
        setActiveImage(primary);

        const activeVariants = data.variants.filter((v) => v.isActive);
        if (activeVariants.length > 0) {
          setSelectedColor(activeVariants[0].color);
          setSelectedSize(activeVariants[0].size);
        }
      })
      .catch((err) => {
        if (err?.response?.status === 404) {
          setNotFound(true);
        }
      })
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8">
        <div className="flex flex-col gap-8 md:flex-row">
          <div className="flex-1">
            <Skeleton className="aspect-square w-full rounded-xl" />
            <div className="mt-2 flex gap-2">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="aspect-square w-16 rounded-lg" />
              ))}
            </div>
          </div>
          <div className="flex flex-1 flex-col gap-3">
            <Skeleton className="h-7 w-3/4" />
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-6 w-1/4" />
            <Skeleton className="h-24 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if (notFound || !product) {
    return (
      <div className="flex flex-col items-center justify-center py-32 text-center">
        <p className="mb-4 text-lg font-medium">Sản phẩm không tồn tại</p>
        <Button variant="outline" onClick={() => router.push("/products")}>
          <ArrowLeftIcon className="size-4" />
          Quay lại danh sách
        </Button>
      </div>
    );
  }

  // Derive unique colors and sizes from active variants
  const activeVariants = product.variants.filter((v) => v.isActive);
  const allColors = [...new Set(product.variants.map((v) => v.color))];
  const allSizes = [...new Set(product.variants.map((v) => v.size))];

  const isColorActive = (color: string) =>
    activeVariants.some((v) => v.color === color);

  const isSizeActive = (size: string) =>
    activeVariants.some(
      (v) => v.size === size && (selectedColor ? v.color === selectedColor : true)
    );

  const selectedVariant: ProductVariant | undefined = product.variants.find(
    (v) =>
      v.color === selectedColor &&
      v.size === selectedSize &&
      v.isActive
  );

  const displayPrice = selectedVariant
    ? formatVND(selectedVariant.price)
    : product.minPrice === product.maxPrice
    ? formatVND(product.minPrice)
    : `${formatVND(product.minPrice)} – ${formatVND(product.maxPrice)}`;

  const images =
    product.images.length > 0
      ? product.images.sort((a, b) => a.sortOrder - b.sortOrder)
      : product.primaryImageUrl
      ? [
          {
            id: "primary",
            url: product.primaryImageUrl,
            sortOrder: 0,
            isPrimary: true,
          },
        ]
      : [];

  async function handleAddToCart() {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    if (!selectedVariant) {
      toast.error("Vui lòng chọn màu sắc và kích cỡ");
      return;
    }
    setAddingToCart(true);
    try {
      await api.post("/v1/cart/items", {
        productVariantId: selectedVariant.id,
        quantity: 1,
      });
      toast.success("Đã thêm vào giỏ hàng");
    } catch {
      toast.error("Không thể thêm vào giỏ hàng. Vui lòng thử lại.");
    } finally {
      setAddingToCart(false);
    }
  }

  const brandName =
    (product as unknown as { brandName?: string }).brandName ??
    product.brand?.name;
  const categoryName =
    (product as unknown as { categoryName?: string }).categoryName ??
    product.category?.name;

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <Button
        variant="ghost"
        size="sm"
        className="mb-6"
        onClick={() => router.push("/products")}
      >
        <ArrowLeftIcon className="size-4" />
        Quay lại
      </Button>

      <div className="flex flex-col gap-8 md:flex-row">
        {/* Image gallery */}
        <div className="flex-1">
          <div className="overflow-hidden rounded-xl border bg-muted">
            {images.length > 0 ? (
              <img
                src={activeImage ?? images[0].url}
                alt={product.name}
                className="aspect-square w-full object-cover"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).src = "/placeholder.png";
                }}
              />
            ) : (
              <div className="flex aspect-square w-full items-center justify-center text-muted-foreground">
                <img
                  src="/placeholder.png"
                  alt="Chưa có ảnh"
                  className="aspect-square w-full object-cover opacity-40"
                />
              </div>
            )}
          </div>

          {/* Thumbnails */}
          {images.length > 1 && (
            <div className="mt-2 flex gap-2 overflow-x-auto pb-1">
              {images.map((img) => (
                <button
                  key={img.id}
                  onClick={() => setActiveImage(img.url)}
                  className={`shrink-0 overflow-hidden rounded-lg border-2 transition-colors ${
                    activeImage === img.url
                      ? "border-primary"
                      : "border-transparent hover:border-muted-foreground/30"
                  }`}
                >
                  <img
                    src={img.url}
                    alt=""
                    className="h-16 w-16 object-cover"
                    onError={(e) => {
                      (e.currentTarget as HTMLImageElement).src =
                        "/placeholder.png";
                    }}
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Product info */}
        <div className="flex flex-1 flex-col gap-4">
          {/* Name and brand */}
          <div>
            {brandName && (
              <p className="mb-1 text-sm text-muted-foreground">{brandName}</p>
            )}
            <h1 className="text-2xl font-bold leading-snug">{product.name}</h1>
          </div>

          {/* Price */}
          <p className="text-2xl font-semibold text-primary">{displayPrice}</p>

          {/* Attributes */}
          <div className="flex flex-wrap gap-2">
            {categoryName && (
              <Badge variant="outline">{categoryName}</Badge>
            )}
            {product.gender && (
              <Badge variant="secondary">
                {genderLabel[product.gender] ?? product.gender}
              </Badge>
            )}
            {product.frameShape && (
              <Badge variant="outline">Gọng: {product.frameShape}</Badge>
            )}
            {product.material && (
              <Badge variant="outline">Chất liệu: {product.material}</Badge>
            )}
          </div>

          {/* Description */}
          {product.description && (
            <p className="text-sm leading-relaxed text-muted-foreground">
              {product.description}
            </p>
          )}

          {/* Color selection */}
          {allColors.length > 0 && (
            <div>
              <p className="mb-2 text-sm font-medium">
                Màu sắc:{" "}
                {selectedColor && (
                  <span className="text-muted-foreground">{selectedColor}</span>
                )}
              </p>
              <div className="flex flex-wrap gap-2">
                {allColors.map((color) => {
                  const active = isColorActive(color);
                  return (
                    <button
                      key={color}
                      disabled={!active}
                      onClick={() => {
                        setSelectedColor(color);
                        // Reset size if current selection is no longer valid
                        const valid = activeVariants.find(
                          (v) => v.color === color && v.size === selectedSize
                        );
                        if (!valid) setSelectedSize(null);
                      }}
                      className={`rounded-lg border px-3 py-1 text-sm transition-all ${
                        selectedColor === color
                          ? "border-primary bg-primary/10 font-semibold"
                          : "border-border hover:border-muted-foreground/50"
                      } disabled:cursor-not-allowed disabled:opacity-40`}
                    >
                      {color}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Size selection */}
          {allSizes.length > 0 && (
            <div>
              <p className="mb-2 text-sm font-medium">Kích cỡ</p>
              <div className="flex flex-wrap gap-2">
                {allSizes.map((size) => {
                  const active = isSizeActive(size);
                  return (
                    <button
                      key={size}
                      disabled={!active}
                      onClick={() => setSelectedSize(size)}
                      className={`rounded-lg border px-3 py-1 text-sm transition-all ${
                        selectedSize === size
                          ? "border-primary bg-primary/10 font-semibold"
                          : "border-border hover:border-muted-foreground/50"
                      } disabled:cursor-not-allowed disabled:opacity-40`}
                    >
                      {size}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Add to cart */}
          <Button
            size="lg"
            className="mt-2 w-full"
            disabled={addingToCart || (activeVariants.length > 0 && !selectedVariant)}
            onClick={handleAddToCart}
          >
            {addingToCart ? "Đang thêm..." : "Thêm vào giỏ hàng"}
          </Button>
        </div>
      </div>
    </div>
  );
}
