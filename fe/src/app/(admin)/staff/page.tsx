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
import {
  getStaff,
  createStaff,
  updateStaff,
  deactivateStaff,
  getBranches,
  type UserResponse,
  type StaffFormData,
} from "@/lib/admin-staff.api";
import type { Branch } from "@/types";

const PAGE_SIZE = 10;

type Role = "SUPER_ADMIN" | "BRANCH_MANAGER" | "STAFF";

const ROLE_LABEL: Record<Role, string> = {
  SUPER_ADMIN: "Super Admin",
  BRANCH_MANAGER: "Quản lý chi nhánh",
  STAFF: "Nhân viên",
};

const ROLE_CLASS: Record<Role, string> = {
  SUPER_ADMIN: "bg-red-100 text-red-800 border-red-200",
  BRANCH_MANAGER: "bg-blue-100 text-blue-800 border-blue-200",
  STAFF: "bg-gray-100 text-gray-600 border-gray-200",
};

const addSchema = z
  .object({
    fullName: z.string().min(1, "Họ tên là bắt buộc"),
    email: z.string().email("Email không hợp lệ"),
    password: z.string().min(6, "Mật khẩu tối thiểu 6 ký tự"),
    role: z.enum(["SUPER_ADMIN", "BRANCH_MANAGER", "STAFF"]),
    branchId: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.role !== "SUPER_ADMIN" && !data.branchId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Chi nhánh là bắt buộc với vai trò này",
        path: ["branchId"],
      });
    }
  });

const editSchema = z
  .object({
    fullName: z.string().min(1, "Họ tên là bắt buộc"),
    email: z.string().email("Email không hợp lệ"),
    password: z.string().optional(),
    role: z.enum(["SUPER_ADMIN", "BRANCH_MANAGER", "STAFF"]),
    branchId: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.role !== "SUPER_ADMIN" && !data.branchId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Chi nhánh là bắt buộc với vai trò này",
        path: ["branchId"],
      });
    }
  });

type AddFormValues = z.infer<typeof addSchema>;
type EditFormValues = z.infer<typeof editSchema>;

export default function AdminStaffPage() {
  const [staff, setStaff] = useState<UserResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [loading, setLoading] = useState(true);

  const [branches, setBranches] = useState<Branch[]>([]);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState<UserResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [deactivateTarget, setDeactivateTarget] = useState<UserResponse | null>(null);
  const [deactivateDialogOpen, setDeactivateDialogOpen] = useState(false);
  const [deactivating, setDeactivating] = useState(false);

  const addForm = useForm<AddFormValues>({
    resolver: zodResolver(addSchema),
    defaultValues: { role: "STAFF", fullName: "", email: "", password: "" },
  });

  const editForm = useForm<EditFormValues>({
    resolver: zodResolver(editSchema),
    defaultValues: { role: "STAFF", fullName: "", email: "", password: "" },
  });

  const isEditing = !!editingStaff;
  const form = isEditing ? editForm : addForm;
  const watchedRole = form.watch("role");

  useEffect(() => {
    getBranches()
      .then(setBranches)
      .catch(() => toast.error("Không thể tải danh sách chi nhánh"));
  }, []);

  function loadStaff(page: number) {
    setLoading(true);
    getStaff(page, PAGE_SIZE)
      .then((res) => {
        setStaff(res.content);
        setTotalPages(res.totalPages);
        setTotalElements(res.totalElements);
        setPageNumber(res.number);
      })
      .catch(() => toast.error("Không thể tải danh sách nhân viên"))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadStaff(pageNumber);
  }, [pageNumber]); // eslint-disable-line react-hooks/exhaustive-deps

  function openAddDialog() {
    setEditingStaff(null);
    addForm.reset({ role: "STAFF", fullName: "", email: "", password: "", branchId: "" });
    setDialogOpen(true);
  }

  function openEditDialog(member: UserResponse) {
    setEditingStaff(member);
    editForm.reset({
      fullName: member.fullName,
      email: member.email,
      password: "",
      role: member.role,
      branchId: member.branchId ?? "",
    });
    setDialogOpen(true);
  }

  async function onSubmitAdd(values: AddFormValues) {
    setSubmitting(true);
    try {
      const payload: StaffFormData = {
        fullName: values.fullName,
        email: values.email,
        password: values.password,
        role: values.role,
        branchId: values.role !== "SUPER_ADMIN" ? values.branchId : undefined,
      };
      await createStaff(payload);
      toast.success("Thêm nhân viên thành công");
      setDialogOpen(false);
      loadStaff(pageNumber);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message;
      toast.error(msg ?? "Đã xảy ra lỗi");
    } finally {
      setSubmitting(false);
    }
  }

  async function onSubmitEdit(values: EditFormValues) {
    if (!editingStaff) return;
    setSubmitting(true);
    try {
      const payload: StaffFormData = {
        fullName: values.fullName,
        email: values.email,
        password: values.password ?? "",
        role: values.role,
        branchId: values.role !== "SUPER_ADMIN" ? values.branchId : undefined,
      };
      await updateStaff(editingStaff.id, payload);
      toast.success("Cập nhật nhân viên thành công");
      setDialogOpen(false);
      loadStaff(pageNumber);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message;
      toast.error(msg ?? "Đã xảy ra lỗi");
    } finally {
      setSubmitting(false);
    }
  }

  function openDeactivateDialog(member: UserResponse) {
    setDeactivateTarget(member);
    setDeactivateDialogOpen(true);
  }

  async function handleDeactivate() {
    if (!deactivateTarget) return;
    setDeactivating(true);
    try {
      await deactivateStaff(deactivateTarget.id);
      toast.success("Đã vô hiệu hóa nhân viên");
      setDeactivateDialogOpen(false);
      setDeactivateTarget(null);
      loadStaff(pageNumber);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message;
      toast.error(msg ?? "Không thể vô hiệu hóa nhân viên");
    } finally {
      setDeactivating(false);
    }
  }

  function getBranchName(branchId: string | null): string {
    if (!branchId) return "—";
    return branches.find((b) => b.id === branchId)?.name ?? "—";
  }

  const activeForm = isEditing ? editForm : addForm;

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-xl font-semibold">Quản lý nhân viên</h1>
        <Button onClick={openAddDialog}>Thêm nhân viên</Button>
      </div>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      ) : staff.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
          <p>Không có nhân viên nào</p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Họ tên</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Vai trò</TableHead>
                  <TableHead>Chi nhánh</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {staff.map((member) => (
                  <TableRow key={member.id}>
                    <TableCell className="font-medium">{member.fullName}</TableCell>
                    <TableCell className="text-muted-foreground">{member.email}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={ROLE_CLASS[member.role]}
                      >
                        {ROLE_LABEL[member.role]}
                      </Badge>
                    </TableCell>
                    <TableCell>{getBranchName(member.branchId)}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={
                          member.isActive
                            ? "bg-green-100 text-green-800 border-green-200"
                            : "bg-gray-100 text-gray-500 border-gray-200"
                        }
                      >
                        {member.isActive ? "Hoạt động" : "Vô hiệu"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openEditDialog(member)}
                        >
                          Sửa
                        </Button>
                        {member.isActive && (
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => openDeactivateDialog(member)}
                          >
                            Vô hiệu hóa
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="mt-6 flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Trang {pageNumber + 1} / {totalPages} — {totalElements} nhân viên
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
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {isEditing ? "Chỉnh sửa nhân viên" : "Thêm nhân viên"}
            </DialogTitle>
          </DialogHeader>

          {isEditing ? (
            <form
              id="staff-form"
              onSubmit={editForm.handleSubmit(onSubmitEdit)}
              noValidate
            >
              <StaffFormFields
                form={editForm}
                branches={branches}
                watchedRole={watchedRole}
                isEditing
              />
            </form>
          ) : (
            <form
              id="staff-form"
              onSubmit={addForm.handleSubmit(onSubmitAdd)}
              noValidate
            >
              <StaffFormFields
                form={addForm}
                branches={branches}
                watchedRole={watchedRole}
                isEditing={false}
              />
            </form>
          )}

          <DialogFooter className="mt-2">
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" form="staff-form" disabled={submitting}>
              {submitting
                ? "Đang lưu..."
                : isEditing
                ? "Cập nhật"
                : "Thêm nhân viên"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Deactivate Confirm Dialog */}
      <Dialog open={deactivateDialogOpen} onOpenChange={setDeactivateDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Vô hiệu hóa nhân viên</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn vô hiệu hóa nhân viên{" "}
            <span className="font-medium text-foreground">
              {deactivateTarget?.fullName}
            </span>
            ?
          </p>
          <DialogFooter className="mt-2">
            <Button
              variant="outline"
              onClick={() => setDeactivateDialogOpen(false)}
              disabled={deactivating}
            >
              Hủy
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeactivate}
              disabled={deactivating}
            >
              {deactivating ? "Đang xử lý..." : "Vô hiệu hóa"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// Extracted form fields component to avoid duplicating JSX for add/edit forms
interface StaffFormFieldsProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any;
  branches: Branch[];
  watchedRole: string;
  isEditing: boolean;
}

function StaffFormFields({ form, branches, watchedRole, isEditing }: StaffFormFieldsProps) {
  const {
    register,
    setValue,
    formState: { errors },
  } = form;

  return (
    <div className="flex flex-col gap-4">
      {/* Họ tên */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="fullName">Họ tên *</Label>
        <Input id="fullName" {...register("fullName")} />
        {errors.fullName && (
          <p className="text-xs text-destructive">{errors.fullName.message}</p>
        )}
      </div>

      {/* Email */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="email">Email *</Label>
        <Input id="email" type="email" {...register("email")} />
        {errors.email && (
          <p className="text-xs text-destructive">{errors.email.message}</p>
        )}
      </div>

      {/* Mật khẩu */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="password">
          {isEditing ? "Mật khẩu mới (để trống nếu không đổi)" : "Mật khẩu *"}
        </Label>
        <Input id="password" type="password" {...register("password")} />
        {errors.password && (
          <p className="text-xs text-destructive">{errors.password.message}</p>
        )}
      </div>

      {/* Vai trò */}
      <div className="flex flex-col gap-1.5">
        <Label>Vai trò *</Label>
        <Select
          onValueChange={(v) =>
            setValue("role", v as Role, { shouldValidate: true })
          }
          defaultValue={form.getValues("role")}
        >
          <SelectTrigger>
            <SelectValue placeholder="Chọn vai trò" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="SUPER_ADMIN">Super Admin</SelectItem>
            <SelectItem value="BRANCH_MANAGER">Quản lý chi nhánh</SelectItem>
            <SelectItem value="STAFF">Nhân viên</SelectItem>
          </SelectContent>
        </Select>
        {errors.role && (
          <p className="text-xs text-destructive">{errors.role.message}</p>
        )}
      </div>

      {/* Chi nhánh — ẩn khi SUPER_ADMIN */}
      {watchedRole !== "SUPER_ADMIN" && (
        <div className="flex flex-col gap-1.5">
          <Label>Chi nhánh *</Label>
          <Select
            onValueChange={(v) =>
              setValue("branchId", v, { shouldValidate: true })
            }
            defaultValue={form.getValues("branchId")}
          >
            <SelectTrigger>
              <SelectValue placeholder="Chọn chi nhánh" />
            </SelectTrigger>
            <SelectContent>
              {branches.map((branch) => (
                <SelectItem key={branch.id} value={branch.id}>
                  {branch.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {errors.branchId && (
            <p className="text-xs text-destructive">{errors.branchId.message}</p>
          )}
        </div>
      )}
    </div>
  );
}
