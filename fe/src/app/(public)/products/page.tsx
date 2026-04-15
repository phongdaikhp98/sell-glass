"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlidersHorizontalIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import ProductCard from "@/components/products/ProductCard";
import { getProducts, getCategories, getBrands } from "@/lib/product.api";
import type { ProductListItem, Category, Brand, PageResponse } from "@/types";

const GENDER_OPTIONS = [
  { value: "", label: "Tất cả" },
  { value: "MEN", label: "Nam" },
  { value: "WOMEN", label: "Nữ" },
  { value: "UNISEX", label: "Unisex" },
];

const PAGE_SIZE = 12;

function buildQuery(params: {
  search: string;
  categoryId: string;
  brandId: string;
  gender: string;
  page: number;
}) {
  const q = new URLSearchParams();
  if (params.search) q.set("search", params.search);
  if (params.categoryId) q.set("categoryId", params.categoryId);
  if (params.brandId) q.set("brandId", params.brandId);
  if (params.gender) q.set("gender", params.gender);
  if (params.page > 0) q.set("page", String(params.page));
  return q.toString();
}

function FilterPanel({
  search,
  onSearchChange,
  categoryId,
  onCategoryChange,
  brandId,
  onBrandChange,
  gender,
  onGenderChange,
  onClear,
  categories,
  brands,
}: {
  search: string;
  onSearchChange: (v: string) => void;
  categoryId: string;
  onCategoryChange: (v: string) => void;
  brandId: string;
  onBrandChange: (v: string) => void;
  gender: string;
  onGenderChange: (v: string) => void;
  onClear: () => void;
  categories: Category[];
  brands: Brand[];
}) {
  return (
    <div className="flex flex-col gap-4">
      <div>
        <label className="mb-1 block text-xs font-medium text-muted-foreground">
          Tìm kiếm
        </label>
        <Input
          placeholder="Tên sản phẩm..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <div>
        <label className="mb-1 block text-xs font-medium text-muted-foreground">
          Danh mục
        </label>
        <Select value={categoryId} onValueChange={onCategoryChange}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="Tất cả danh mục" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Tất cả danh mục</SelectItem>
            {categories.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div>
        <label className="mb-1 block text-xs font-medium text-muted-foreground">
          Thương hiệu
        </label>
        <Select value={brandId} onValueChange={onBrandChange}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="Tất cả thương hiệu" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Tất cả thương hiệu</SelectItem>
            {brands.map((b) => (
              <SelectItem key={b.id} value={b.id}>
                {b.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div>
        <label className="mb-1 block text-xs font-medium text-muted-foreground">
          Giới tính
        </label>
        <Select value={gender} onValueChange={onGenderChange}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="Tất cả" />
          </SelectTrigger>
          <SelectContent>
            {GENDER_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Button variant="outline" size="sm" onClick={onClear}>
        Xóa bộ lọc
      </Button>
    </div>
  );
}

export default function ProductsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Local state mirrors URL params
  const [search, setSearch] = useState(searchParams.get("search") ?? "");
  const [categoryId, setCategoryId] = useState(
    searchParams.get("categoryId") ?? ""
  );
  const [brandId, setBrandId] = useState(searchParams.get("brandId") ?? "");
  const [gender, setGender] = useState(searchParams.get("gender") ?? "");
  const [page, setPage] = useState(Number(searchParams.get("page") ?? "0"));

  const [result, setResult] = useState<PageResponse<ProductListItem> | null>(
    null
  );
  const [loading, setLoading] = useState(true);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  // Debounce search: push URL only after 400ms pause
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isFirstRender = useRef(true);

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(() => {
      const q = buildQuery({ search, categoryId, brandId, gender, page: 0 });
      setPage(0);
      router.push(`/products${q ? `?${q}` : ""}`);
    }, 400);
    return () => {
      if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    };
  }, [search]); // eslint-disable-line react-hooks/exhaustive-deps

  // Fetch categories and brands once
  useEffect(() => {
    getCategories().then(setCategories).catch(() => {});
    getBrands().then(setBrands).catch(() => {});
  }, []);

  // Fetch products whenever URL search params change
  useEffect(() => {
    const currentSearch = searchParams.get("search") ?? "";
    const currentCategory = searchParams.get("categoryId") ?? "";
    const currentBrand = searchParams.get("brandId") ?? "";
    const currentGender = searchParams.get("gender") ?? "";
    const currentPage = Number(searchParams.get("page") ?? "0");

    setLoading(true);
    getProducts({
      search: currentSearch || undefined,
      categoryId: currentCategory || undefined,
      brandId: currentBrand || undefined,
      gender: currentGender || undefined,
      page: currentPage,
      size: PAGE_SIZE,
    })
      .then(setResult)
      .catch(() => setResult(null))
      .finally(() => setLoading(false));
  }, [searchParams]);

  const stateSnapshot = { search, categoryId, brandId, gender, page };

  function navigate(overrides: Partial<typeof stateSnapshot>) {
    const merged = { ...stateSnapshot, ...overrides };
    const q = buildQuery(merged);
    router.push(`/products${q ? `?${q}` : ""}`);
  }

  function handleCategoryChange(v: string) {
    setCategoryId(v);
    navigate({ categoryId: v, page: 0 });
    setPage(0);
  }

  function handleBrandChange(v: string) {
    setBrandId(v);
    navigate({ brandId: v, page: 0 });
    setPage(0);
  }

  function handleGenderChange(v: string) {
    setGender(v);
    navigate({ gender: v, page: 0 });
    setPage(0);
  }

  function handleClear() {
    setSearch("");
    setCategoryId("");
    setBrandId("");
    setGender("");
    setPage(0);
    isFirstRender.current = true; // prevent debounce effect from firing
    router.push("/products");
  }

  function handlePageChange(newPage: number) {
    setPage(newPage);
    navigate({ page: newPage });
  }

  const filterProps = {
    search,
    onSearchChange: setSearch,
    categoryId,
    onCategoryChange: handleCategoryChange,
    brandId,
    onBrandChange: handleBrandChange,
    gender,
    onGenderChange: handleGenderChange,
    onClear: handleClear,
    categories,
    brands,
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Sản phẩm</h1>
        {/* Mobile filter toggle */}
        <Sheet open={mobileFilterOpen} onOpenChange={setMobileFilterOpen}>
          <SheetTrigger
            render={
              <Button variant="outline" size="sm" className="md:hidden" />
            }
          >
            <SlidersHorizontalIcon className="size-4" />
            Bộ lọc
          </SheetTrigger>
          <SheetContent side="left">
            <SheetHeader>
              <SheetTitle>Bộ lọc</SheetTitle>
            </SheetHeader>
            <div className="p-4">
              <FilterPanel {...filterProps} />
            </div>
          </SheetContent>
        </Sheet>
      </div>

      <div className="flex gap-6">
        {/* Sidebar filter — desktop only */}
        <aside className="hidden w-56 shrink-0 md:block">
          <FilterPanel {...filterProps} />
        </aside>

        {/* Product grid */}
        <div className="flex-1">
          {loading ? (
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="flex flex-col gap-2">
                  <Skeleton className="aspect-square w-full rounded-xl" />
                  <Skeleton className="h-3 w-2/3" />
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-1/2" />
                </div>
              ))}
            </div>
          ) : !result || result.content.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
              <p className="text-base">Không tìm thấy sản phẩm</p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
                {result.content.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              {/* Pagination */}
              <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
                <p className="text-sm text-muted-foreground">
                  Trang {result.number + 1} / {result.totalPages} —{" "}
                  {result.totalElements} sản phẩm
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={result.number === 0}
                    onClick={() => handlePageChange(result.number - 1)}
                  >
                    Trước
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={result.number + 1 >= result.totalPages}
                    onClick={() => handlePageChange(result.number + 1)}
                  >
                    Tiếp
                  </Button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
