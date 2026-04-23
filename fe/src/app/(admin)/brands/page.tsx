"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";

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
  getBrands,
  createBrand,
  updateBrand,
  deleteBrand,
  toSlug,
  type BrandResponse,
} from "@/lib/admin-catalog.api";

const schema = z.object({
  name: z.string().min(1, "Tên là bắt buộc"),
  slug: z.string().min(1, "Slug là bắt buộc"),
  logoUrl: z.string().url("URL không hợp lệ").or(z.literal("")).optional(),
});

type FormValues = z.infer<typeof schema>;

export default function AdminBrandsPage() {
  const [brands, setBrands] = useState<BrandResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<BrandResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<BrandResponse | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", slug: "", logoUrl: "" },
  });

  const watchedName = form.watch("name");

  useEffect(() => {
    if (!editing) {
      form.setValue("slug", toSlug(watchedName), { shouldValidate: false });
    }
  }, [watchedName, editing]); // eslint-disable-line react-hooks/exhaustive-deps

  function load() {
    setLoading(true);
    getBrands()
      .then(setBrands)
      .catch(() => toast.error("Không thể tải thương hiệu"))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function openAddDialog() {
    setEditing(null);
    form.reset({ name: "", slug: "", logoUrl: "" });
    setDialogOpen(true);
  }

  function openEditDialog(brand: BrandResponse) {
    setEditing(brand);
    form.reset({ name: brand.name, slug: brand.slug, logoUrl: brand.logoUrl ?? "" });
    setDialogOpen(true);
  }

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      const payload = {
        name: values.name,
        slug: values.slug,
        logoUrl: values.logoUrl || undefined,
      };
      if (editing) {
        await updateBrand(editing.id, payload);
        toast.success("Đã cập nhật thương hiệu");
      } else {
        await createBrand(payload);
        toast.success("Đã thêm thương hiệu");
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
      await deleteBrand(deleteTarget.id);
      toast.success("Đã xóa thương hiệu");
      setDeleteDialogOpen(false);
      setDeleteTarget(null);
      load();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Không thể xóa thương hiệu");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Quản lý thương hiệu</h1>
        <Button onClick={openAddDialog}>Thêm thương hiệu</Button>
      </div>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      ) : brands.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
          <p>Chưa có thương hiệu nào</p>
        </div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Logo</TableHead>
                <TableHead>Tên thương hiệu</TableHead>
                <TableHead>Slug</TableHead>
                <TableHead className="text-right">Hành động</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {brands.map((brand) => (
                <TableRow key={brand.id}>
                  <TableCell>
                    {brand.logoUrl ? (
                      /* eslint-disable-next-line @next/next/no-img-element */
                      <img
                        src={brand.logoUrl}
                        alt={brand.name}
                        className="h-8 w-8 rounded object-contain"
                      />
                    ) : (
                      <div className="flex h-8 w-8 items-center justify-center rounded bg-muted text-xs text-muted-foreground">
                        —
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="font-medium">{brand.name}</TableCell>
                  <TableCell className="font-mono text-sm text-muted-foreground">
                    {brand.slug}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => openEditDialog(brand)}>
                        Sửa
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => { setDeleteTarget(brand); setDeleteDialogOpen(true); }}
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
            <DialogTitle>{editing ? "Chỉnh sửa thương hiệu" : "Thêm thương hiệu"}</DialogTitle>
          </DialogHeader>
          <form id="brand-form" onSubmit={form.handleSubmit(onSubmit)} noValidate>
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="name">Tên thương hiệu *</Label>
                <Input id="name" {...form.register("name")} />
                {form.formState.errors.name && (
                  <p className="text-xs text-destructive">{form.formState.errors.name.message}</p>
                )}
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="slug">Slug *</Label>
                <Input id="slug" {...form.register("slug")} />
                {form.formState.errors.slug && (
                  <p className="text-xs text-destructive">{form.formState.errors.slug.message}</p>
                )}
                <p className="text-xs text-muted-foreground">Tự động tạo từ tên, có thể chỉnh sửa</p>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="logoUrl">URL Logo (tùy chọn)</Label>
                <Input id="logoUrl" placeholder="https://..." {...form.register("logoUrl")} />
                {form.formState.errors.logoUrl && (
                  <p className="text-xs text-destructive">{form.formState.errors.logoUrl.message}</p>
                )}
              </div>
            </div>
          </form>
          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setDialogOpen(false)}>Hủy</Button>
            <Button type="submit" form="brand-form" disabled={submitting}>
              {submitting ? "Đang lưu..." : editing ? "Cập nhật" : "Thêm"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Xóa thương hiệu</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn xóa thương hiệu{" "}
            <span className="font-medium text-foreground">{deleteTarget?.name}</span>? Hành động này không thể hoàn tác.
          </p>
          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)} disabled={deleting}>
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
