"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import ProductCard from "@/components/products/ProductCard";
import { getProducts } from "@/lib/product.api";
import { getBranches } from "@/lib/branch.api";
import type { ProductListItem, Branch } from "@/types";

export default function HomePage() {
  const [products, setProducts] = useState<ProductListItem[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [branchesLoading, setBranchesLoading] = useState(true);

  useEffect(() => {
    getProducts({ size: 8, sort: "createdAt,desc" })
      .then((res) => setProducts(res.content))
      .catch(() => toast.error("Không thể tải sản phẩm"))
      .finally(() => setProductsLoading(false));
  }, []);

  useEffect(() => {
    getBranches()
      .then((data) => setBranches(data.filter((b) => b.isActive)))
      .catch(() => toast.error("Không thể tải danh sách chi nhánh"))
      .finally(() => setBranchesLoading(false));
  }, []);

  return (
    <div className="flex flex-col">
      {/* Hero */}
      <section className="bg-gradient-to-br from-blue-50 to-indigo-100 py-24 px-4">
        <div className="mx-auto max-w-3xl text-center">
          <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl lg:text-6xl">
            Sell Glass
          </h1>
          <p className="mt-4 text-lg text-gray-600 sm:text-xl">
            Kính mắt chất lượng — Giá tốt nhất
          </p>
          <p className="mt-3 text-base text-gray-500">
            Hệ thống chuỗi cửa hàng kính mắt uy tín với hàng trăm mẫu mã đa dạng,
            phù hợp mọi phong cách và nhu cầu.
          </p>
          <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <Button asChild size="lg" className="w-full sm:w-auto">
              <Link href="/products">Xem sản phẩm</Link>
            </Button>
            <Button
              asChild
              size="lg"
              variant="outline"
              className="w-full sm:w-auto"
            >
              <Link href="/branches">Tìm chi nhánh</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Sản phẩm nổi bật */}
      <section className="py-16 px-4">
        <div className="mx-auto max-w-6xl">
          <div className="mb-8 flex items-center justify-between">
            <h2 className="text-2xl font-semibold text-gray-900">
              Sản phẩm mới nhất
            </h2>
            <Link
              href="/products"
              className="text-sm font-medium text-primary hover:underline"
            >
              Xem tất cả &rarr;
            </Link>
          </div>

          {productsLoading ? (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="flex flex-col gap-2">
                  <Skeleton className="aspect-square w-full rounded-xl" />
                  <Skeleton className="h-4 w-3/4 rounded" />
                  <Skeleton className="h-4 w-1/2 rounded" />
                </div>
              ))}
            </div>
          ) : products.length === 0 ? (
            <p className="text-center text-muted-foreground py-12">
              Chưa có sản phẩm nào.
            </p>
          ) : (
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Chi nhánh */}
      <section className="bg-gray-50 py-16 px-4">
        <div className="mx-auto max-w-6xl">
          <div className="mb-8 flex items-center justify-between">
            <h2 className="text-2xl font-semibold text-gray-900">
              Hệ thống cửa hàng
            </h2>
            <Link
              href="/branches"
              className="text-sm font-medium text-primary hover:underline"
            >
              Xem tất cả chi nhánh &rarr;
            </Link>
          </div>

          {branchesLoading ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="rounded-xl border bg-white p-5 flex flex-col gap-3">
                  <Skeleton className="h-5 w-2/3 rounded" />
                  <Skeleton className="h-4 w-full rounded" />
                  <Skeleton className="h-4 w-1/2 rounded" />
                  <Skeleton className="h-4 w-1/3 rounded" />
                </div>
              ))}
            </div>
          ) : branches.length === 0 ? (
            <p className="text-center text-muted-foreground py-12">
              Chưa có chi nhánh nào.
            </p>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {branches.map((branch) => (
                <div
                  key={branch.id}
                  className="rounded-xl border bg-white p-5 flex flex-col gap-2 shadow-sm hover:shadow-md transition-shadow"
                >
                  <h3 className="font-semibold text-gray-900">{branch.name}</h3>
                  <p className="text-sm text-gray-600">{branch.address}</p>
                  {branch.phone && (
                    <p className="text-sm text-gray-600">
                      <span className="font-medium">SĐT:</span> {branch.phone}
                    </p>
                  )}
                  {branch.openTime && branch.closeTime && (
                    <p className="text-sm text-gray-500">
                      <span className="font-medium">Giờ mở cửa:</span>{" "}
                      {branch.openTime} – {branch.closeTime}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* CTA cuối trang */}
      <section className="py-20 px-4 bg-gradient-to-r from-indigo-600 to-blue-600">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-2xl font-bold text-white sm:text-3xl">
            Đặt lịch thử kính ngay hôm nay
          </h2>
          <p className="mt-3 text-indigo-100">
            Trải nghiệm dịch vụ thử kính miễn phí tại cửa hàng gần nhất.
          </p>
          <div className="mt-8">
            <Button
              asChild
              size="lg"
              variant="secondary"
              className="font-semibold"
            >
              <Link href="/appointments">Đặt lịch ngay</Link>
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
