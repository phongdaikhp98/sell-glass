"use client";

import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import type { ProductListItem } from "@/types";

const genderLabel: Record<ProductListItem["gender"], string> = {
  MEN: "Nam",
  WOMEN: "Nữ",
  UNISEX: "Unisex",
};

const formatVND = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    amount
  );

export default function ProductCard({ product }: { product: ProductListItem }) {
  const brandName =
    (product as unknown as { brandName?: string }).brandName ??
    product.brand?.name;

  const priceDisplay =
    product.minPrice === product.maxPrice
      ? formatVND(product.minPrice)
      : `${formatVND(product.minPrice)} – ${formatVND(product.maxPrice)}`;

  return (
    <Link
      href={`/products/${product.slug}`}
      className="group flex flex-col overflow-hidden rounded-xl border bg-card transition-shadow hover:shadow-md"
    >
      {/* Image */}
      <div className="relative aspect-square overflow-hidden bg-muted">
        <img
          src={product.primaryImageUrl ?? "/placeholder.png"}
          alt={product.name}
          className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          onError={(e) => {
            (e.currentTarget as HTMLImageElement).src = "/placeholder.png";
          }}
        />
        <Badge
          className="absolute top-2 left-2"
          variant="secondary"
        >
          {genderLabel[product.gender]}
        </Badge>
      </div>

      {/* Info */}
      <div className="flex flex-col gap-1 p-3">
        {brandName && (
          <p className="text-xs text-muted-foreground">{brandName}</p>
        )}
        <p className="line-clamp-2 text-sm font-medium leading-snug">
          {product.name}
        </p>
        <p className="mt-1 text-sm font-semibold text-primary">{priceDisplay}</p>
      </div>
    </Link>
  );
}
