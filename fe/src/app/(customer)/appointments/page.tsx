"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
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
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { useAuthStore } from "@/store/auth.store";
import { getBranches } from "@/lib/branch.api";
import {
  getMyAppointments,
  createAppointment,
  cancelAppointment,
  type AppointmentResponse,
} from "@/lib/appointment.api";
import type { Branch, PageResponse } from "@/types";

const PAGE_SIZE = 10;

const APPT_STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  DONE: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const APPT_STATUS_CLASS: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800 border-yellow-200",
  CONFIRMED: "bg-blue-100 text-blue-800 border-blue-200",
  DONE: "bg-green-100 text-green-800 border-green-200",
  CANCELLED: "bg-red-100 text-red-800 border-red-200",
};

export default function AppointmentsPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();

  // Form state
  const [branches, setBranches] = useState<Branch[]>([]);
  const [branchId, setBranchId] = useState("");
  const [scheduledAt, setScheduledAt] = useState("");
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // List state
  const [result, setResult] = useState<PageResponse<AppointmentResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  // Cancel dialog
  const [cancelTarget, setCancelTarget] = useState<AppointmentResponse | null>(null);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    getBranches().then(setBranches).catch(() => toast.error("Không thể tải danh sách chi nhánh"));
  }, [isAuthenticated]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!isAuthenticated) return;
    setLoading(true);
    getMyAppointments(page, PAGE_SIZE)
      .then(setResult)
      .catch(() => toast.error("Không thể tải danh sách lịch hẹn"))
      .finally(() => setLoading(false));
  }, [isAuthenticated, page]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!branchId) {
      toast.error("Vui lòng chọn chi nhánh");
      return;
    }
    if (!scheduledAt) {
      toast.error("Vui lòng chọn ngày giờ");
      return;
    }
    const selected = new Date(scheduledAt);
    if (selected <= new Date()) {
      toast.error("Ngày giờ hẹn phải ở tương lai");
      return;
    }
    setSubmitting(true);
    try {
      await createAppointment({
        branchId,
        scheduledAt: selected.toISOString(),
        note: note.trim() || undefined,
      });
      toast.success("Đặt lịch hẹn thành công");
      setBranchId("");
      setScheduledAt("");
      setNote("");
      // Reload list
      setPage(0);
      getMyAppointments(0, PAGE_SIZE)
        .then(setResult)
        .catch(() => {});
    } catch {
      toast.error("Đặt lịch hẹn thất bại");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel() {
    if (!cancelTarget) return;
    setCancelling(true);
    try {
      await cancelAppointment(cancelTarget.id);
      toast.success("Hủy lịch hẹn thành công");
      setCancelTarget(null);
      setResult((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          content: prev.content.map((a) =>
            a.id === cancelTarget.id ? { ...a, status: "CANCELLED" } : a
          ),
        };
      });
    } catch {
      toast.error("Hủy lịch hẹn thất bại");
    } finally {
      setCancelling(false);
    }
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 flex flex-col gap-6">
      {/* Book appointment form */}
      <Card>
        <CardHeader>
          <CardTitle>Đặt lịch hẹn</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="branch-select">Chi nhánh</Label>
              <Select value={branchId} onValueChange={(v) => { if (v !== null) setBranchId(v); }}>
                <SelectTrigger id="branch-select" className="w-full">
                  <SelectValue placeholder="Chọn chi nhánh" />
                </SelectTrigger>
                <SelectContent>
                  {branches.map((b) => (
                    <SelectItem key={b.id} value={b.id}>
                      {b.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="scheduled-at">Ngày giờ hẹn</Label>
              <Input
                id="scheduled-at"
                type="datetime-local"
                value={scheduledAt}
                onChange={(e) => setScheduledAt(e.target.value)}
                min={new Date().toISOString().slice(0, 16)}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="note">Ghi chú (tùy chọn)</Label>
              <Textarea
                id="note"
                placeholder="Nhập ghi chú nếu có..."
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={3}
              />
            </div>

            <Button type="submit" disabled={submitting} className="self-start">
              {submitting ? "Đang đặt..." : "Đặt lịch hẹn"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {/* Appointment list */}
      <Card>
        <CardHeader>
          <CardTitle>Lịch hẹn của tôi</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex flex-col gap-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-12 w-full rounded-md" />
              ))}
            </div>
          ) : !result || result.content.length === 0 ? (
            <p className="py-12 text-center text-muted-foreground">Bạn chưa có lịch hẹn nào</p>
          ) : (
            <>
              <div className="rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Chi nhánh</TableHead>
                      <TableHead>Ngày giờ</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Ghi chú</TableHead>
                      <TableHead className="text-right">Hành động</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {result.content.map((appt) => (
                      <TableRow key={appt.id}>
                        <TableCell className="font-medium">{appt.branchName}</TableCell>
                        <TableCell className="text-muted-foreground text-sm">
                          {new Date(appt.scheduledAt).toLocaleString("vi-VN")}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant="outline"
                            className={APPT_STATUS_CLASS[appt.status] ?? ""}
                          >
                            {APPT_STATUS_LABEL[appt.status] ?? appt.status}
                          </Badge>
                        </TableCell>
                        <TableCell className="max-w-40 truncate text-sm text-muted-foreground">
                          {appt.note ?? "—"}
                        </TableCell>
                        <TableCell className="text-right">
                          {(appt.status === "PENDING" || appt.status === "CONFIRMED") && (
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-destructive hover:text-destructive"
                              onClick={() => setCancelTarget(appt)}
                            >
                              Hủy
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              <div className="mt-4 flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
                <p className="text-sm text-muted-foreground">
                  Trang {result.number + 1} / {result.totalPages} — {result.totalElements} lịch hẹn
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={result.number === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    Trước
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={result.number + 1 >= result.totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Tiếp
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Cancel confirm dialog */}
      <Dialog open={!!cancelTarget} onOpenChange={(open) => { if (!open) setCancelTarget(null); }}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Xác nhận hủy lịch hẹn</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn hủy lịch hẹn tại{" "}
            <span className="font-semibold text-foreground">{cancelTarget?.branchName}</span>{" "}
            vào{" "}
            <span className="font-semibold text-foreground">
              {cancelTarget ? new Date(cancelTarget.scheduledAt).toLocaleString("vi-VN") : ""}
            </span>
            ?
          </p>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setCancelTarget(null)}
              disabled={cancelling}
            >
              Không
            </Button>
            <Button
              variant="destructive"
              onClick={handleCancel}
              disabled={cancelling}
            >
              {cancelling ? "Đang hủy..." : "Hủy lịch"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
