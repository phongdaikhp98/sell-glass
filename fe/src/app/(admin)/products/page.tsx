"use client";

import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import {
  getAdminProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  type ProductFormData,
  type AdminProductListItem,
} from "@/lib/admin-product.api";
import { getCategories, getBrands } from "@/lib/product.api";
import ProductImageManager from "@/components/products/ProductImageManager";
import type { Category, Brand } from "@/types";

const formatVND = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    price
  );

function generateSlug(str: string): string {
  return str
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

const GENDER_LABEL: Record<string, string> = {
  MEN: "Nam",
  WOMEN: "Nữ",
  UNISEX: "Unisex",
};

const GENDER_CLASS: Record<string, string> = {
  MEN: "bg-blue-100 text-blue-800 border-blue-200",
  WOMEN: "bg-pink-100 text-pink-800 border-pink-200",
  UNISEX: "bg-purple-100 text-purple-800 border-purple-200",
};

const PAGE_SIZE = 12;

const schema = z.object({
  name: z.string().min(1, "Tên sản phẩm là bắt buộc"),
  slug: z.string().min(1, "Slug là bắt buộc"),
  categoryId: z.string().min(1, "Danh mục là bắt buộc"),
  brandId: z.string().min(1, "Thương hiệu là bắt buộc"),
  description: z.string().optional(),
  frameShape: z.string().optional(),
  material: z.string().optional(),
  gender: z.enum(["MEN", "WOMEN", "UNISEX"]),
  isActive: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

export default function AdminProductsPage() {
  const [products, setProducts] = useState<AdminProductListItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");

  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<AdminProductListItem | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<AdminProductListItem | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const [imageProduct, setImageProduct] = useState<AdminProductListItem | null>(null);
  const [imageDialogOpen, setImageDialogOpen] = useState(false);

  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { isActive: true, gender: "UNISEX" },
  });

  const nameValue = watch("name");

  useEffect(() => {
    Promise.all([getCategories(), getBrands()])
      .then(([cats, brs]) => {
        setCategories(cats);
        setBrands(brs);
      })
      .catch(() => toast.error("Không thể tải danh mục / thương hiệu"));
  }, []);

  useEffect(() => {
    setLoading(true);
    getAdminProducts(pageNumber, PAGE_SIZE, search || undefined)
      .then((res) => {
        setProducts(res.content);
        setTotalPages(res.totalPages);
        setTotalElements(res.totalElements);
        setPageNumber(res.number);
      })
      .catch(() => toast.error("Không thể tải danh sách sản phẩm"))
      .finally(() => setLoading(false));
  }, [pageNumber, search]);

  function handleSearchChange(value: string) {
    setSearchInput(value);
    if (searchTimeout.current) clearTimeout(searchTimeout.current);
    searchTimeout.current = setTimeout(() => {
      setSearch(value);
      setPageNumber(0);
    }, 400);
  }

  function openAddDialog() {
    setEditingProduct(null);
    reset({ isActive: true, gender: "UNISEX", name: "", slug: "", categoryId: "", brandId: "" });
    setDialogOpen(true);
  }

  function openEditDialog(product: AdminProductListItem) {
    setEditingProduct(product);
    reset({
      name: product.name,
      slug: product.slug,
      categoryId: product.category.id,
      brandId: product.brand.id,
      gender: product.gender,
      isActive: product.isActive ?? true,
      description: "",
      frameShape: "",
      material: "",
    });
    setDialogOpen(true);
  }

  // Auto-generate slug when name changes (only on add, not edit)
  useEffect(() => {
    if (!editingProduct && nameValue) {
      setValue("slug", generateSlug(nameValue), { shouldValidate: false });
    }
  }, [nameValue, editingProduct]); // eslint-disable-line react-hooks/exhaustive-deps

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      const payload: ProductFormData = {
        name: values.name,
        slug: values.slug,
        categoryId: values.categoryId,
        brandId: values.brandId,
        description: values.description,
        frameShape: values.frameShape,
        material: values.material,
        gender: values.gender,
        isActive: values.isActive,
      };

      if (editingProduct) {
        await updateProduct(editingProduct.id, payload);
        toast.success("Cập nhật sản phẩm thành công");
      } else {
        await createProduct(payload);
        toast.success("Thêm sản phẩm thành công");
      }

      setDialogOpen(false);
      setLoading(true);
      getAdminProducts(pageNumber, PAGE_SIZE, search || undefined)
        .then((res) => {
          setProducts(res.content);
          setTotalPages(res.totalPages);
          setTotalElements(res.totalElements);
        })
        .catch(() => toast.error("Không thể tải lại danh sách"))
        .finally(() => setLoading(false));
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message;
      toast.error(msg ?? "Đã xảy ra lỗi");
    } finally {
      setSubmitting(false);
    }
  }

  function openDeleteDialog(product: AdminProductListItem) {
    setDeleteTarget(product);
    setDeleteDialogOpen(true);
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteProduct(deleteTarget.id);
      toast.success("Xóa sản phẩm thành công");
      setDeleteDialogOpen(false);
      setDeleteTarget(null);
      setLoading(true);
      getAdminProducts(pageNumber, PAGE_SIZE, search || undefined)
        .then((res) => {
          setProducts(res.content);
          setTotalPages(res.totalPages);
          setTotalElements(res.totalElements);
          if (res.content.length === 0 && pageNumber > 0) {
            setPageNumber((p) => p - 1);
          }
        })
        .catch(() => toast.error("Không thể tải lại danh sách"))
        .finally(() => setLoading(false));
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message;
      toast.error(msg ?? "Không thể xóa sản phẩm");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-xl font-semibold">Quản lý sản phẩm</h1>
        <Button onClick={openAddDialog}>Thêm sản phẩm</Button>
      </div>

      <div className="mb-4">
        <Input
          placeholder="Tìm kiếm sản phẩm..."
          value={searchInput}
          onChange={(e) => handleSearchChange(e.target.value)}
          className="max-w-sm"
        />
      </div>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      ) : products.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
          <p>Không có sản phẩm nào</p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tên</TableHead>
                  <TableHead>Slug</TableHead>
                  <TableHead>Danh mục</TableHead>
                  <TableHead>Thương hiệu</TableHead>
                  <TableHead>Giới tính</TableHead>
                  <TableHead>Giá thấp nhất</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {products.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell className="font-medium">{product.name}</TableCell>
                    <TableCell className="font-mono text-sm text-muted-foreground">
                      {product.slug}
                    </TableCell>
                    <TableCell>{product.category.name}</TableCell>
                    <TableCell>{product.brand.name}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={GENDER_CLASS[product.gender]}
                      >
                        {GENDER_LABEL[product.gender]}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatVND(product.minPrice)}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={
                          product.isActive
                            ? "bg-green-100 text-green-800 border-green-200"
                            : "bg-gray-100 text-gray-500 border-gray-200"
                        }
                      >
                        {product.isActive ? "Hoạt động" : "Ẩn"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setImageProduct(product);
                            setImageDialogOpen(true);
                          }}
                        >
                          Ảnh
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openEditDialog(product)}
                        >
                          Sửa
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => openDeleteDialog(product)}
                        >
                          Xóa
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="mt-6 flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Trang {pageNumber + 1} / {totalPages} — {totalElements} sản phẩm
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={pageNumber === 0}
                onClick={() => setPageNumber((p) => p - 1)}
              >
                Trước
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={pageNumber + 1 >= totalPages}
                onClick={() => setPageNumber((p) => p + 1)}
              >
                Tiếp
              </Button>
            </div>
          </div>
        </>
      )}

      {/* Add / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>
              {editingProduct ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm"}
            </DialogTitle>
          </DialogHeader>

          <form id="product-form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="flex flex-col gap-4">
              {/* Tên */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="name">Tên sản phẩm *</Label>
                <Input id="name" {...register("name")} />
                {errors.name && (
                  <p className="text-xs text-destructive">{errors.name.message}</p>
                )}
              </div>

              {/* Slug */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="slug">Slug *</Label>
                <Input id="slug" {...register("slug")} />
                {errors.slug && (
                  <p className="text-xs text-destructive">{errors.slug.message}</p>
                )}
              </div>

              {/* Danh mục */}
              <div className="flex flex-col gap-1.5">
                <Label>Danh mục *</Label>
                <Select
                  onValueChange={(v) => setValue("categoryId", v, { shouldValidate: true })}
                  defaultValue={editingProduct?.category.id}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn danh mục" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={cat.id}>
                        {cat.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.categoryId && (
                  <p className="text-xs text-destructive">{errors.categoryId.message}</p>
                )}
              </div>

              {/* Thương hiệu */}
              <div className="flex flex-col gap-1.5">
                <Label>Thương hiệu *</Label>
                <Select
                  onValueChange={(v) => setValue("brandId", v, { shouldValidate: true })}
                  defaultValue={editingProduct?.brand.id}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn thương hiệu" />
                  </SelectTrigger>
                  <SelectContent>
                    {brands.map((brand) => (
                      <SelectItem key={brand.id} value={brand.id}>
                        {brand.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.brandId && (
                  <p className="text-xs text-destructive">{errors.brandId.message}</p>
                )}
              </div>

              {/* Mô tả */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="description">Mô tả</Label>
                <Textarea id="description" rows={3} {...register("description")} />
              </div>

              {/* Hình dáng gọng */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="frameShape">Hình dáng gọng</Label>
                <Input id="frameShape" {...register("frameShape")} />
              </div>

              {/* Chất liệu */}
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="material">Chất liệu</Label>
                <Input id="material" {...register("material")} />
              </div>

              {/* Giới tính */}
              <div className="flex flex-col gap-1.5">
                <Label>Giới tính *</Label>
                <Select
                  onValueChange={(v) =>
                    setValue("gender", v as "MEN" | "WOMEN" | "UNISEX", { shouldValidate: true })
                  }
                  defaultValue={editingProduct?.gender ?? "UNISEX"}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn giới tính" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MEN">Nam</SelectItem>
                    <SelectItem value="WOMEN">Nữ</SelectItem>
                    <SelectItem value="UNISEX">Unisex</SelectItem>
                  </SelectContent>
                </Select>
                {errors.gender && (
                  <p className="text-xs text-destructive">{errors.gender.message}</p>
                )}
              </div>

              {/* Kích hoạt */}
              <div className="flex items-center gap-2">
                <input
                  id="isActive"
                  type="checkbox"
                  className="h-4 w-4 rounded border-gray-300"
                  {...register("isActive")}
                />
                <Label htmlFor="isActive">Kích hoạt</Label>
              </div>
            </div>
          </form>

          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" form="product-form" disabled={submitting}>
              {submitting
                ? "Đang lưu..."
                : editingProduct
                ? "Cập nhật"
                : "Thêm sản phẩm"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Image Management Dialog */}
      <Dialog open={imageDialogOpen} onOpenChange={setImageDialogOpen}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>Quản lý hình ảnh</DialogTitle>
          </DialogHeader>
          {imageProduct && (
            <ProductImageManager
              productId={imageProduct.id}
              productName={imageProduct.name}
            />
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Xóa sản phẩm</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn xóa sản phẩm{" "}
            <span className="font-medium text-foreground">
              {deleteTarget?.name}
            </span>
            ? Hành động này không thể hoàn tác.
          </p>
          <DialogFooter className="mt-2">
            <Button
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
              disabled={deleting}
            >
              Hủy
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              {deleting ? "Đang xóa..." : "Xóa"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
