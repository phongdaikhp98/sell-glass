"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAuthStore } from "@/store/auth.store";
import {
  getProfile,
  getAddresses,
  addAddress,
  updateAddress,
  deleteAddress,
} from "@/lib/profile.api";
import type { Customer } from "@/types";

interface CustomerAddress {
  id: string;
  receiverName: string;
  phone: string;
  address: string;
  isDefault: boolean;
}

const addressSchema = z.object({
  receiverName: z.string().min(1, "Vui lòng nhập tên người nhận"),
  phone: z.string().min(1, "Vui lòng nhập số điện thoại"),
  address: z.string().min(1, "Vui lòng nhập địa chỉ"),
  isDefault: z.boolean(),
});

type AddressFormValues = z.infer<typeof addressSchema>;

export default function ProfilePage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();

  const [profile, setProfile] = useState<Customer | null>(null);
  const [profileLoading, setProfileLoading] = useState(true);

  const [addresses, setAddresses] = useState<CustomerAddress[]>([]);
  const [addressesLoading, setAddressesLoading] = useState(true);

  // Edit profile dialog
  const [editProfileOpen, setEditProfileOpen] = useState(false);
  const [profileFullName, setProfileFullName] = useState("");
  const [profilePhone, setProfilePhone] = useState("");
  const [profileSaving, setProfileSaving] = useState(false);

  // Address dialog
  const [addressDialogOpen, setAddressDialogOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<CustomerAddress | null>(null);
  const [addressSaving, setAddressSaving] = useState(false);

  // Confirm delete dialog
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const addressForm = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: { receiverName: "", phone: "", address: "", isDefault: false },
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }

    getProfile()
      .then((data: Customer) => {
        setProfile(data);
        setProfileFullName(data.fullName);
        setProfilePhone(data.phone ?? "");
      })
      .catch(() => toast.error("Không thể tải thông tin cá nhân"))
      .finally(() => setProfileLoading(false));

    getAddresses()
      .then((data: CustomerAddress[]) => setAddresses(data))
      .catch(() => toast.error("Không thể tải danh sách địa chỉ"))
      .finally(() => setAddressesLoading(false));
  }, [isAuthenticated]); // eslint-disable-line react-hooks/exhaustive-deps

  // Profile edit handlers
  function openEditProfile() {
    setProfileFullName(profile?.fullName ?? "");
    setProfilePhone(profile?.phone ?? "");
    setEditProfileOpen(true);
  }

  async function handleSaveProfile() {
    // PUT /v1/me not yet implemented — disable for now
    setProfileSaving(true);
    try {
      toast.info("Tính năng cập nhật hồ sơ chưa được hỗ trợ.");
    } finally {
      setProfileSaving(false);
      setEditProfileOpen(false);
    }
  }

  // Address handlers
  function openAddAddress() {
    setEditingAddress(null);
    addressForm.reset({ receiverName: "", phone: "", address: "", isDefault: false });
    setAddressDialogOpen(true);
  }

  function openEditAddress(addr: CustomerAddress) {
    setEditingAddress(addr);
    addressForm.reset({
      receiverName: addr.receiverName,
      phone: addr.phone,
      address: addr.address,
      isDefault: addr.isDefault,
    });
    setAddressDialogOpen(true);
  }

  async function handleAddressSubmit(values: AddressFormValues) {
    setAddressSaving(true);
    try {
      if (editingAddress) {
        const updated = await updateAddress(editingAddress.id, values);
        setAddresses((prev) =>
          prev.map((a) => (a.id === editingAddress.id ? updated : a))
        );
        toast.success("Đã cập nhật địa chỉ");
      } else {
        const created = await addAddress(values);
        setAddresses((prev) => [...prev, created]);
        toast.success("Đã thêm địa chỉ");
      }
      setAddressDialogOpen(false);
    } catch {
      toast.error("Có lỗi xảy ra, vui lòng thử lại");
    } finally {
      setAddressSaving(false);
    }
  }

  function confirmDelete(id: string) {
    setDeleteTargetId(id);
    setDeleteConfirmOpen(true);
  }

  async function handleDelete() {
    if (!deleteTargetId) return;
    setDeleting(true);
    try {
      await deleteAddress(deleteTargetId);
      setAddresses((prev) => prev.filter((a) => a.id !== deleteTargetId));
      toast.success("Đã xóa địa chỉ");
      setDeleteConfirmOpen(false);
    } catch {
      toast.error("Có lỗi xảy ra khi xóa địa chỉ");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Tài khoản của tôi</h1>

      {/* Thông tin cá nhân */}
      <div className="rounded-xl border bg-card p-6 flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-base">Thông tin cá nhân</h2>
          <Button variant="outline" size="sm" onClick={openEditProfile}>
            Sửa
          </Button>
        </div>
        <Separator />
        {profileLoading ? (
          <div className="flex flex-col gap-3">
            <Skeleton className="h-4 w-1/2 rounded" />
            <Skeleton className="h-4 w-2/3 rounded" />
            <Skeleton className="h-4 w-1/3 rounded" />
          </div>
        ) : profile ? (
          <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2 text-sm">
            <div>
              <dt className="text-muted-foreground">Họ và tên</dt>
              <dd className="font-medium mt-0.5">{profile.fullName}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Email</dt>
              <dd className="font-medium mt-0.5">{profile.email}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Số điện thoại</dt>
              <dd className="font-medium mt-0.5">{profile.phone ?? "Chưa cập nhật"}</dd>
            </div>
          </dl>
        ) : (
          <p className="text-sm text-muted-foreground">Không thể tải thông tin.</p>
        )}
      </div>

      {/* Quản lý địa chỉ */}
      <div className="rounded-xl border bg-card p-6 flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-base">Địa chỉ của tôi</h2>
          <Button size="sm" onClick={openAddAddress}>
            Thêm địa chỉ
          </Button>
        </div>
        <Separator />
        {addressesLoading ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 2 }).map((_, i) => (
              <Skeleton key={i} className="h-16 w-full rounded-lg" />
            ))}
          </div>
        ) : addresses.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-6">
            Bạn chưa có địa chỉ nào.
          </p>
        ) : (
          <div className="flex flex-col gap-3">
            {addresses.map((addr) => (
              <div
                key={addr.id}
                className="rounded-lg border px-4 py-3 flex flex-col gap-1.5 sm:flex-row sm:items-start sm:justify-between"
              >
                <div className="flex flex-col gap-0.5 text-sm">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{addr.receiverName}</span>
                    {addr.isDefault && (
                      <Badge variant="secondary" className="text-xs">
                        Mặc định
                      </Badge>
                    )}
                  </div>
                  <span className="text-muted-foreground">{addr.phone}</span>
                  <span className="text-muted-foreground">{addr.address}</span>
                </div>
                <div className="flex gap-2 self-start sm:self-center">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => openEditAddress(addr)}
                  >
                    Sửa
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive hover:text-destructive"
                    onClick={() => confirmDelete(addr.id)}
                  >
                    Xóa
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Lịch sử đơn hàng */}
      <div className="rounded-xl border bg-card p-6">
        <Link
          href="/orders"
          className="flex items-center justify-between text-sm font-medium hover:text-primary transition-colors"
        >
          <span>Xem lịch sử đơn hàng</span>
          <span>&rarr;</span>
        </Link>
      </div>

      {/* Dialog sửa thông tin cá nhân */}
      <Dialog open={editProfileOpen} onOpenChange={setEditProfileOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Chỉnh sửa thông tin</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4 pt-2">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-name">Họ và tên</Label>
              <Input
                id="edit-name"
                value={profileFullName}
                onChange={(e) => setProfileFullName(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-email">Email</Label>
              <Input id="edit-email" value={profile?.email ?? ""} disabled />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-phone">Số điện thoại</Label>
              <Input
                id="edit-phone"
                value={profilePhone}
                onChange={(e) => setProfilePhone(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                onClick={() => setEditProfileOpen(false)}
                disabled={profileSaving}
              >
                Hủy
              </Button>
              <Button onClick={handleSaveProfile} disabled={profileSaving}>
                Lưu
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Dialog thêm / sửa địa chỉ */}
      <Dialog open={addressDialogOpen} onOpenChange={setAddressDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {editingAddress ? "Chỉnh sửa địa chỉ" : "Thêm địa chỉ mới"}
            </DialogTitle>
          </DialogHeader>
          <form
            onSubmit={addressForm.handleSubmit(handleAddressSubmit)}
            className="flex flex-col gap-4 pt-2"
          >
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="addr-name">Tên người nhận</Label>
              <Input
                id="addr-name"
                {...addressForm.register("receiverName")}
              />
              {addressForm.formState.errors.receiverName && (
                <p className="text-xs text-destructive">
                  {addressForm.formState.errors.receiverName.message}
                </p>
              )}
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="addr-phone">Số điện thoại</Label>
              <Input id="addr-phone" {...addressForm.register("phone")} />
              {addressForm.formState.errors.phone && (
                <p className="text-xs text-destructive">
                  {addressForm.formState.errors.phone.message}
                </p>
              )}
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="addr-address">Địa chỉ</Label>
              <Input id="addr-address" {...addressForm.register("address")} />
              {addressForm.formState.errors.address && (
                <p className="text-xs text-destructive">
                  {addressForm.formState.errors.address.message}
                </p>
              )}
            </div>
            <div className="flex items-center gap-2">
              <input
                id="addr-default"
                type="checkbox"
                className="h-4 w-4 rounded border-gray-300"
                {...addressForm.register("isDefault")}
              />
              <Label htmlFor="addr-default" className="cursor-pointer">
                Đặt làm địa chỉ mặc định
              </Label>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => setAddressDialogOpen(false)}
                disabled={addressSaving}
              >
                Hủy
              </Button>
              <Button type="submit" disabled={addressSaving}>
                {editingAddress ? "Lưu thay đổi" : "Thêm"}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* Dialog xác nhận xóa */}
      <Dialog open={deleteConfirmOpen} onOpenChange={setDeleteConfirmOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Xác nhận xóa</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn xóa địa chỉ này không? Hành động này không thể hoàn tác.
          </p>
          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              onClick={() => setDeleteConfirmOpen(false)}
              disabled={deleting}
            >
              Hủy
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              Xóa
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
