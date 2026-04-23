"use client";

import { useEffect, useState } from "react";
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
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  getBranches,
  createBranch,
  updateBranch,
  type BranchRequest,
} from "@/lib/admin-catalog.api";
import type { Branch } from "@/types";

const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)$/;

const schema = z.object({
  name: z.string().min(1, "Tên chi nhánh là bắt buộc"),
  address: z.string().min(1, "Địa chỉ là bắt buộc"),
  phone: z.string().min(1, "Số điện thoại là bắt buộc"),
  openTime: z.string().regex(timeRegex, "Định dạng HH:mm"),
  closeTime: z.string().regex(timeRegex, "Định dạng HH:mm"),
});

type FormValues = z.infer<typeof schema>;

export default function AdminBranchesPage() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Branch | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [toggleTarget, setToggleTarget] = useState<Branch | null>(null);
  const [toggleDialogOpen, setToggleDialogOpen] = useState(false);
  const [toggling, setToggling] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", address: "", phone: "", openTime: "08:00", closeTime: "21:00" },
  });

  function load() {
    setLoading(true);
    getBranches()
      .then(setBranches)
      .catch(() => toast.error("Không thể tải danh sách chi nhánh"))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function openAddDialog() {
    setEditing(null);
    form.reset({ name: "", address: "", phone: "", openTime: "08:00", closeTime: "21:00" });
    setDialogOpen(true);
  }

  function openEditDialog(branch: Branch) {
    setEditing(branch);
    form.reset({
      name: branch.name,
      address: branch.address,
      phone: branch.phone,
      openTime: branch.openTime,
      closeTime: branch.closeTime,
    });
    setDialogOpen(true);
  }

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      const payload: BranchRequest = values;
      if (editing) {
        await updateBranch(editing.id, { ...payload, isActive: editing.isActive });
        toast.success("Đã cập nhật chi nhánh");
      } else {
        await createBranch(payload);
        toast.success("Đã thêm chi nhánh");
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

  async function handleToggleActive() {
    if (!toggleTarget) return;
    setToggling(true);
    try {
      await updateBranch(toggleTarget.id, {
        name: toggleTarget.name,
        address: toggleTarget.address,
        phone: toggleTarget.phone,
        openTime: toggleTarget.openTime,
        closeTime: toggleTarget.closeTime,
        isActive: !toggleTarget.isActive,
      });
      toast.success(toggleTarget.isActive ? "Đã vô hiệu hóa chi nhánh" : "Đã kích hoạt chi nhánh");
      setToggleDialogOpen(false);
      setToggleTarget(null);
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Không thể cập nhật trạng thái");
    } finally {
      setToggling(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Quản lý chi nhánh</h1>
        <Button onClick={openAddDialog}>Thêm chi nhánh</Button>
      </div>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full rounded-md" />
          ))}
        </div>
      ) : branches.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
          <p>Chưa có chi nhánh nào</p>
        </div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tên chi nhánh</TableHead>
                <TableHead>Địa chỉ</TableHead>
                <TableHead>SĐT</TableHead>
                <TableHead>Giờ mở cửa</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="text-right">Hành động</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {branches.map((branch) => (
                <TableRow key={branch.id}>
                  <TableCell className="font-medium">{branch.name}</TableCell>
                  <TableCell className="max-w-xs truncate text-muted-foreground">
                    {branch.address}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{branch.phone}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {branch.openTime} – {branch.closeTime}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="outline"
                      className={
                        branch.isActive
                          ? "bg-green-100 text-green-800 border-green-200"
                          : "bg-gray-100 text-gray-500 border-gray-200"
                      }
                    >
                      {branch.isActive ? "Hoạt động" : "Vô hiệu"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => openEditDialog(branch)}>
                        Sửa
                      </Button>
                      <Button
                        variant={branch.isActive ? "destructive" : "outline"}
                        size="sm"
                        onClick={() => { setToggleTarget(branch); setToggleDialogOpen(true); }}
                      >
                        {branch.isActive ? "Vô hiệu hóa" : "Kích hoạt"}
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
            <DialogTitle>{editing ? "Chỉnh sửa chi nhánh" : "Thêm chi nhánh"}</DialogTitle>
          </DialogHeader>
          <form id="branch-form" onSubmit={form.handleSubmit(onSubmit)} noValidate>
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="name">Tên chi nhánh *</Label>
                <Input id="name" {...form.register("name")} />
                {form.formState.errors.name && (
                  <p className="text-xs text-destructive">{form.formState.errors.name.message}</p>
                )}
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="address">Địa chỉ *</Label>
                <Input id="address" {...form.register("address")} />
                {form.formState.errors.address && (
                  <p className="text-xs text-destructive">{form.formState.errors.address.message}</p>
                )}
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="phone">Số điện thoại *</Label>
                <Input id="phone" {...form.register("phone")} />
                {form.formState.errors.phone && (
                  <p className="text-xs text-destructive">{form.formState.errors.phone.message}</p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="openTime">Giờ mở cửa *</Label>
                  <Input id="openTime" placeholder="08:00" {...form.register("openTime")} />
                  {form.formState.errors.openTime && (
                    <p className="text-xs text-destructive">{form.formState.errors.openTime.message}</p>
                  )}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="closeTime">Giờ đóng cửa *</Label>
                  <Input id="closeTime" placeholder="21:00" {...form.register("closeTime")} />
                  {form.formState.errors.closeTime && (
                    <p className="text-xs text-destructive">{form.formState.errors.closeTime.message}</p>
                  )}
                </div>
              </div>
            </div>
          </form>
          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setDialogOpen(false)}>Hủy</Button>
            <Button type="submit" form="branch-form" disabled={submitting}>
              {submitting ? "Đang lưu..." : editing ? "Cập nhật" : "Thêm"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Toggle Active Confirm Dialog */}
      <Dialog open={toggleDialogOpen} onOpenChange={setToggleDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>
              {toggleTarget?.isActive ? "Vô hiệu hóa chi nhánh" : "Kích hoạt chi nhánh"}
            </DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn{" "}
            {toggleTarget?.isActive ? "vô hiệu hóa" : "kích hoạt"} chi nhánh{" "}
            <span className="font-medium text-foreground">{toggleTarget?.name}</span>?
            {toggleTarget?.isActive && " Khách hàng sẽ không thể đặt lịch hoặc đặt hàng tại chi nhánh này."}
          </p>
          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setToggleDialogOpen(false)} disabled={toggling}>
              Hủy
            </Button>
            <Button
              variant={toggleTarget?.isActive ? "destructive" : "default"}
              onClick={handleToggleActive}
              disabled={toggling}
            >
              {toggling ? "Đang xử lý..." : toggleTarget?.isActive ? "Vô hiệu hóa" : "Kích hoạt"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
