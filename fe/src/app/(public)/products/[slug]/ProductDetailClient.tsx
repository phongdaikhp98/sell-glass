"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeftIcon, GlassesIcon, StarIcon } from "lucide-react";
import TryOnModal from "@/components/products/TryOnModal";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import { getProductBySlug } from "@/lib/product.api";
import { getProductReviews, createReview } from "@/lib/review.api";
import { useAuthStore } from "@/store/auth.store";
import api from "@/lib/api";
import type { Product, ProductVariant, Review } from "@/types";

const formatVND = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    amount
  );

const genderLabel: Record<string, string> = {
  MEN: "Nam",
  WOMEN: "Nữ",
  UNISEX: "Unisex",
};

export default function ProductDetailClient({ slug }: { slug: string }) {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();

  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [activeImage, setActiveImage] = useState<string | null>(null);
  const [selectedColor, setSelectedColor] = useState<string | null>(null);
  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [addingToCart, setAddingToCart] = useState(false);
  const [tryOnOpen, setTryOnOpen] = useState(false);

  // Reviews
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewsTotal, setReviewsTotal] = useState(0);
  const [reviewsPage, setReviewsPage] = useState(1);
  const [reviewsTotalPages, setReviewsTotalPages] = useState(1);
  const [loadingReviews, setLoadingReviews] = useState(false);
  const [myRating, setMyRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [myComment, setMyComment] = useState("");
  const [submittingReview, setSubmittingReview] = useState(false);

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

  const loadReviews = useCallback(async (productId: string, page: number) => {
    setLoadingReviews(true);
    try {
      const data = await getProductReviews(productId, page, 5);
      setReviews(data.content);
      setReviewsTotal(data.totalElements);
      setReviewsTotalPages(data.totalPages || 1);
    } catch {
      // silently ignore
    } finally {
      setLoadingReviews(false);
    }
  }, []);

  useEffect(() => {
    if (product) loadReviews(product.id, reviewsPage);
  }, [product, reviewsPage, loadReviews]);

  async function handleSubmitReview() {
    if (!isAuthenticated) { router.push("/login"); return; }
    if (myRating === 0) { toast.error("Vui lòng chọn số sao"); return; }
    if (!product) return;
    setSubmittingReview(true);
    try {
      await createReview(product.id, { rating: myRating, comment: myComment.trim() || undefined });
      toast.success("Đã gửi đánh giá");
      setMyRating(0);
      setMyComment("");
      setReviewsPage(1);
      loadReviews(product.id, 1);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Không thể gửi đánh giá");
    } finally {
      setSubmittingReview(false);
    }
  }

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
      ? [{ id: "primary", url: product.primaryImageUrl, sortOrder: 0, isPrimary: true }]
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

      <div className="flex flex-col gap-8 md:flex-row" id="product-main">
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
                      (e.currentTarget as HTMLImageElement).src = "/placeholder.png";
                    }}
                  />
                </button>
              ))}
            </div>
          )}

          {/* Try-on button */}
          {images.length > 0 && (
            <button
              onClick={() => setTryOnOpen(true)}
              className="mt-3 flex w-full items-center justify-center gap-2 rounded-md border border-input py-2 text-sm hover:bg-muted transition-colors"
            >
              <GlassesIcon className="h-4 w-4" />
              Thử kính ảo
            </button>
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
            {categoryName && <Badge variant="outline">{categoryName}</Badge>}
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

      {/* Reviews section */}
      <Separator className="my-10" />
      <div>
        <h2 className="mb-6 text-xl font-semibold">
          Đánh giá ({reviewsTotal})
        </h2>

        {/* Write review */}
        <div className="mb-8 rounded-lg border p-4">
          <h3 className="mb-3 font-medium">Viết đánh giá của bạn</h3>
          {isAuthenticated ? (
            <div className="flex flex-col gap-3">
              <div className="flex gap-1">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(0)}
                    onClick={() => setMyRating(star)}
                    className="focus:outline-none"
                  >
                    <StarIcon
                      className={`size-7 transition-colors ${
                        star <= (hoverRating || myRating)
                          ? "fill-yellow-400 text-yellow-400"
                          : "text-muted-foreground"
                      }`}
                    />
                  </button>
                ))}
                {myRating > 0 && (
                  <span className="ml-2 self-center text-sm text-muted-foreground">
                    {["", "Rất tệ", "Tệ", "Bình thường", "Tốt", "Xuất sắc"][myRating]}
                  </span>
                )}
              </div>
              <textarea
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                rows={3}
                placeholder="Chia sẻ trải nghiệm của bạn về sản phẩm này... (tùy chọn)"
                value={myComment}
                onChange={(e) => setMyComment(e.target.value)}
              />
              <div className="flex justify-end">
                <Button
                  size="sm"
                  onClick={handleSubmitReview}
                  disabled={submittingReview || myRating === 0}
                >
                  {submittingReview ? "Đang gửi..." : "Gửi đánh giá"}
                </Button>
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              <button
                className="underline hover:text-foreground"
                onClick={() => router.push("/login")}
              >
                Đăng nhập
              </button>{" "}
              để viết đánh giá. Chỉ khách hàng đã mua và nhận hàng mới có thể đánh giá.
            </p>
          )}
        </div>

        {/* Review list */}
        {loadingReviews ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex flex-col gap-2">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-12 w-full" />
              </div>
            ))}
          </div>
        ) : reviews.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            Chưa có đánh giá nào. Hãy là người đầu tiên!
          </p>
        ) : (
          <div className="space-y-6">
            {reviews.map((review) => (
              <div key={review.id} className="flex flex-col gap-1.5">
                <div className="flex items-center gap-3">
                  <span className="font-medium text-sm">{review.customerName}</span>
                  <div className="flex gap-0.5">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <StarIcon
                        key={star}
                        className={`size-4 ${
                          star <= review.rating
                            ? "fill-yellow-400 text-yellow-400"
                            : "text-muted-foreground/30"
                        }`}
                      />
                    ))}
                  </div>
                  <span className="text-xs text-muted-foreground">
                    {new Date(review.createdAt).toLocaleDateString("vi-VN")}
                  </span>
                </div>
                {review.comment && (
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    {review.comment}
                  </p>
                )}
                <Separator className="mt-3" />
              </div>
            ))}

            {reviewsTotalPages > 1 && (
              <div className="flex items-center justify-center gap-3 pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={reviewsPage === 1}
                  onClick={() => setReviewsPage((p) => p - 1)}
                >
                  Trước
                </Button>
                <span className="text-sm text-muted-foreground">
                  {reviewsPage} / {reviewsTotalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={reviewsPage === reviewsTotalPages}
                  onClick={() => setReviewsPage((p) => p + 1)}
                >
                  Sau
                </Button>
              </div>
            )}
          </div>
        )}
      </div>

      <TryOnModal
        open={tryOnOpen}
        onClose={() => setTryOnOpen(false)}
        glassesImageUrl={activeImage ?? images[0]?.url ?? ""}
        productName={product.name}
      />
    </div>
  );
}
