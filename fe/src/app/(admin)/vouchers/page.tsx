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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  getAdminVouchers,
  createVoucher,
  updateVoucher,
  deleteVoucher,
} from "@/lib/voucher.api";
import type { Voucher } from "@/types";

const formatVND = (n: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(n);

const schema = z.object({
  code: z.string().min(1, "Code là bắt buộc"),
  type: z.enum(["PERCENTAGE", "FIXED_AMOUNT"]),
  value: z.coerce.number().positive("Giá trị phải lớn hơn 0"),
  maxDiscountAmount: z.coerce.number().positive().nullable().optional(),
  minOrderAmount: z.coerce.number().min(0),
  usageLimit: z.coerce.number().int().positive().nullable().optional(),
  expiresAt: z.string().optional().nullable(),
  isActive: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

export default function VouchersPage() {
  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Voucher | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<Voucher | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      code: "",
      type: "PERCENTAGE",
      value: 0,
      maxDiscountAmount: null,
      minOrderAmount: 0,
      usageLimit: null,
      expiresAt: null,
      isActive: true,
    },
  });

  const watchType = form.watch("type");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAdminVouchers(page - 1, 20, search || undefined);
      setVouchers(data.content);
      setTotalPages(data.totalPages || 1);
    } catch {
      toast.error("Không thể tải danh sách voucher");
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => { load(); }, [load]);

  function openAddDialog() {
    setEditing(null);
    form.reset({
      code: "", type: "PERCENTAGE", value: 0,
      maxDiscountAmount: null, minOrderAmount: 0,
      usageLimit: null, expiresAt: null, isActive: true,
    });
    setDialogOpen(true);
  }

  function openEditDialog(v: Voucher) {
    setEditing(v);
    form.reset({
      code: v.code,
      type: v.type,
      value: v.value,
      maxDiscountAmount: v.maxDiscountAmount ?? null,
      minOrderAmount: v.minOrderAmount,
      usageLimit: v.usageLimit ?? null,
      expiresAt: v.expiresAt ? v.expiresAt.slice(0, 16) : null,
      isActive: v.isActive,
    });
    setDialogOpen(true);
  }

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      const payload = {
        ...values,
        maxDiscountAmount: values.maxDiscountAmount || null,
        usageLimit: values.usageLimit || null,
        expiresAt: values.expiresAt || null,
      };
      if (editing) {
        await updateVoucher(editing.id, payload);
        toast.success("Đã cập nhật voucher");
      } else {
        await createVoucher(payload);
        toast.success("Đã tạo voucher");
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
      await deleteVoucher(deleteTarget.id);
      toast.success("Đã xóa voucher");
      setDeleteDialogOpen(false);
      setDeleteTarget(null);
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Không thể xóa voucher");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Voucher</h1>
        <Button onClick={openAddDialog}>Thêm voucher</Button>
      </div>

      <div className="flex gap-3">
        <Input
          placeholder="Tìm theo mã..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(1); }}
          className="max-w-xs"
        />
      </div>

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã</TableHead>
              <TableHead>Loại</TableHead>
              <TableHead>Giá trị</TableHead>
              <TableHead>Min đơn</TableHead>
              <TableHead>Đã dùng</TableHead>
              <TableHead>Hết hạn</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Hành động</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={8} className="py-8 text-center text-sm text-muted-foreground">
                  Đang tải...
                </TableCell>
              </TableRow>
            ) : vouchers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="py-8 text-center text-sm text-muted-foreground">
                  Chưa có voucher nào
                </TableCell>
              </TableRow>
            ) : vouchers.map((v) => (
              <TableRow key={v.id}>
                <TableCell className="font-mono font-semibold">{v.code}</TableCell>
                <TableCell>
                  <Badge variant="outline">
                    {v.type === "PERCENTAGE" ? "%" : "Cố định"}
                  </Badge>
                </TableCell>
                <TableCell>
                  {v.type === "PERCENTAGE"
                    ? `${v.value}%${v.maxDiscountAmount ? ` (tối đa ${formatVND(v.maxDiscountAmount)})` : ""}`
                    : formatVND(v.value)}
                </TableCell>
                <TableCell>{v.minOrderAmount > 0 ? formatVND(v.minOrderAmount) : "—"}</TableCell>
                <TableCell>
                  {v.timesUsed}{v.usageLimit ? `/${v.usageLimit}` : ""}
                </TableCell>
                <TableCell>
                  {v.expiresAt
                    ? new Date(v.expiresAt).toLocaleDateString("vi-VN")
                    : "—"}
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

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <Button variant="outline" size="sm" disabled={page === 1} onClick={() => setPage(p => p - 1)}>
            Trước
          </Button>
          <span className="text-sm text-muted-foreground">{page} / {totalPages}</span>
          <Button variant="outline" size="sm" disabled={page === totalPages} onClick={() => setPage(p => p + 1)}>
            Sau
          </Button>
        </div>
      )}

      {/* Add / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{editing ? "Chỉnh sửa voucher" : "Thêm voucher"}</DialogTitle>
          </DialogHeader>
          <form id="voucher-form" onSubmit={form.handleSubmit(onSubmit)} noValidate>
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="code">Mã voucher *</Label>
                <Input
                  id="code"
                  placeholder="VD: SUMMER20"
                  className="font-mono uppercase"
                  {...form.register("code")}
                  onChange={(e) => form.setValue("code", e.target.value.toUpperCase())}
                />
                {form.formState.errors.code && (
                  <p className="text-xs text-destructive">{form.formState.errors.code.message}</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label>Loại giảm giá *</Label>
                  <Select
                    value={form.watch("type")}
                    onValueChange={(v) => form.setValue("type", v as "PERCENTAGE" | "FIXED_AMOUNT")}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="PERCENTAGE">Phần trăm (%)</SelectItem>
                      <SelectItem value="FIXED_AMOUNT">Số tiền cố định</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="value">
                    {watchType === "PERCENTAGE" ? "Phần trăm (%) *" : "Số tiền giảm (đ) *"}
                  </Label>
                  <Input id="value" type="number" min={0} {...form.register("value")} />
                  {form.formState.errors.value && (
                    <p className="text-xs text-destructive">{form.formState.errors.value.message}</p>
                  )}
                </div>
              </div>

              {watchType === "PERCENTAGE" && (
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="maxDiscountAmount">Giảm tối đa (đ)</Label>
                  <Input id="maxDiscountAmount" type="number" min={0} placeholder="Để trống = không giới hạn" {...form.register("maxDiscountAmount")} />
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="minOrderAmount">Đơn tối thiểu (đ)</Label>
                  <Input id="minOrderAmount" type="number" min={0} {...form.register("minOrderAmount")} />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="usageLimit">Giới hạn lượt dùng</Label>
                  <Input id="usageLimit" type="number" min={1} placeholder="Để trống = không giới hạn" {...form.register("usageLimit")} />
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="expiresAt">Ngày hết hạn</Label>
                <Input id="expiresAt" type="datetime-local" {...form.register("expiresAt")} />
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
            <Button type="submit" form="voucher-form" disabled={submitting}>
              {submitting ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Xóa voucher</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn xóa voucher{" "}
            <span className="font-mono font-medium text-foreground">{deleteTarget?.code}</span>?
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
