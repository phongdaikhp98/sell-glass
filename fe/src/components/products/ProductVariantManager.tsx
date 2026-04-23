"use client";

import { useCallback, useEffect, useState } from "react";
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  getVariants,
  createVariant,
  updateVariant,
  deleteVariant,
  type VariantResponse,
} from "@/lib/admin-product.api";

const formatVND = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

const schema = z.object({
  sku: z.string().min(1, "SKU là bắt buộc"),
  color: z.string().optional(),
  size: z.string().optional(),
  price: z.coerce.number().positive("Giá phải lớn hơn 0"),
  stock: z.coerce.number().int().min(0, "Tồn kho không được âm"),
  isActive: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  productId: string;
  productName: string;
}

export default function ProductVariantManager({ productId, productName }: Props) {
  const [variants, setVariants] = useState<VariantResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<VariantResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<VariantResponse | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { sku: "", color: "", size: "", price: 0, stock: 0, isActive: true },
  });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setVariants(await getVariants(productId));
    } catch {
      toast.error("Không thể tải biến thể");
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => { load(); }, [load]);

  function openAddDialog() {
    setEditing(null);
    form.reset({ sku: "", color: "", size: "", price: 0, stock: 0, isActive: true });
    setDialogOpen(true);
  }

  function openEditDialog(v: VariantResponse) {
    setEditing(v);
    form.reset({ sku: v.sku, color: v.color ?? "", size: v.size ?? "", price: v.price, stock: v.stock, isActive: v.isActive });
    setDialogOpen(true);
  }

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      const payload = { ...values, color: values.color || undefined, size: values.size || undefined };
      if (editing) {
        await updateVariant(productId, editing.id, payload);
        toast.success("Đã cập nhật biến thể");
      } else {
        await createVariant(productId, payload);
        toast.success("Đã thêm biến thể");
      }
      setDialogOpen(false);
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Đã xảy ra lỗi");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteVariant(productId, deleteTarget.id);
      toast.success("Đã xóa biến thể");
      setDeleteDialogOpen(false);
      setDeleteTarget(null);
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Không thể xóa biến thể");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {variants.length} biến thể — {productName}
        </p>
        <Button size="sm" onClick={openAddDialog}>Thêm biến thể</Button>
      </div>

      {loading ? (
        <div className="py-8 text-center text-sm text-muted-foreground">Đang tải...</div>
      ) : variants.length === 0 ? (
        <div className="py-8 text-center text-sm text-muted-foreground">Chưa có biến thể nào</div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>SKU</TableHead>
                <TableHead>Màu</TableHead>
                <TableHead>Size</TableHead>
                <TableHead>Giá</TableHead>
                <TableHead>Tồn kho</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="text-right">Hành động</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {variants.map((v) => (
                <TableRow key={v.id}>
                  <TableCell className="font-mono text-sm">{v.sku}</TableCell>
                  <TableCell>{v.color || "—"}</TableCell>
                  <TableCell>{v.size || "—"}</TableCell>
                  <TableCell>{formatVND(v.price)}</TableCell>
                  <TableCell>
                    <span className={v.stock === 0 ? "font-semibold text-destructive" : ""}>
                      {v.stock}
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="outline"
                      className={v.isActive
                        ? "bg-green-100 text-green-800 border-green-200"
                        : "bg-gray-100 text-gray-500 border-gray-200"}
                    >
                      {v.isActive ? "Hoạt động" : "Ẩn"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => openEditDialog(v)}>Sửa</Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => { setDeleteTarget(v); setDeleteDialogOpen(true); }}
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
      )}

      {/* Add / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? "Chỉnh sửa biến thể" : "Thêm biến thể"}</DialogTitle>
          </DialogHeader>
          <form id="variant-form" onSubmit={form.handleSubmit(onSubmit)} noValidate>
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="sku">SKU *</Label>
                <Input id="sku" {...form.register("sku")} />
                {form.formState.errors.sku && (
                  <p className="text-xs text-destructive">{form.formState.errors.sku.message}</p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="color">Màu sắc</Label>
                  <Input id="color" placeholder="Đen, Nâu..." {...form.register("color")} />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="size">Size</Label>
                  <Input id="size" placeholder="S, M, L..." {...form.register("size")} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="price">Giá (VND) *</Label>
                  <Input id="price" type="number" min={0} {...form.register("price")} />
                  {form.formState.errors.price && (
                    <p className="text-xs text-destructive">{form.formState.errors.price.message}</p>
                  )}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="stock">Tồn kho *</Label>
                  <Input id="stock" type="number" min={0} {...form.register("stock")} />
                  {form.formState.errors.stock && (
                    <p className="text-xs text-destructive">{form.formState.errors.stock.message}</p>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-2">
                <input
                  id="isActive"
                  type="checkbox"
                  className="h-4 w-4 rounded border-gray-300"
                  {...form.register("isActive")}
                />
                <Label htmlFor="isActive">Kích hoạt</Label>
              </div>
            </div>
          </form>
          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setDialogOpen(false)}>Hủy</Button>
            <Button type="submit" form="variant-form" disabled={submitting}>
              {submitting ? "Đang lưu..." : editing ? "Cập nhật" : "Thêm"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Xóa biến thể</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn xóa biến thể <span className="font-medium text-foreground">{deleteTarget?.sku}</span>?
          </p>
          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)} disabled={deleting}>Hủy</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              {deleting ? "Đang xóa..." : "Xóa"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
