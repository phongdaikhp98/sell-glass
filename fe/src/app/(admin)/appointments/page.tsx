"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { MoreHorizontal } from "lucide-react";
import {
  getAdminAppointments,
  updateAppointmentStatus,
  type AppointmentResponse,
} from "@/lib/appointment.api";
import type { PageResponse } from "@/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from "@/components/ui/dropdown-menu";

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

export default function AdminAppointmentsPage() {
  const [result, setResult] = useState<PageResponse<AppointmentResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  // Done dialog (needs resultNote)
  const [doneTarget, setDoneTarget] = useState<AppointmentResponse | null>(null);
  const [resultNote, setResultNote] = useState("");
  const [doneUpdating, setDoneUpdating] = useState(false);

  // Quick status update (confirm / cancel) — no extra input needed
  const [quickTarget, setQuickTarget] = useState<{ appt: AppointmentResponse; status: string } | null>(null);
  const [quickUpdating, setQuickUpdating] = useState(false);

  useEffect(() => {
    setLoading(true);
    getAdminAppointments(page, PAGE_SIZE)
      .then(setResult)
      .catch(() => toast.error("Không thể tải danh sách lịch hẹn"))
      .finally(() => setLoading(false));
  }, [page]);

  function applyUpdate(updated: AppointmentResponse) {
    setResult((prev) => {
      if (!prev) return prev;
      return {
        ...prev,
        content: prev.content.map((a) => (a.id === updated.id ? updated : a)),
      };
    });
  }

  async function handleQuickUpdate() {
    if (!quickTarget) return;
    setQuickUpdating(true);
    try {
      const updated = await updateAppointmentStatus(quickTarget.appt.id, quickTarget.status);
      toast.success("Cập nhật trạng thái thành công");
      setQuickTarget(null);
      applyUpdate(updated);
    } catch {
      toast.error("Cập nhật trạng thái thất bại");
    } finally {
      setQuickUpdating(false);
    }
  }

  async function handleDone() {
    if (!doneTarget) return;
    setDoneUpdating(true);
    try {
      const updated = await updateAppointmentStatus(
        doneTarget.id,
        "DONE",
        resultNote.trim() || undefined
      );
      toast.success("Cập nhật trạng thái thành công");
      setDoneTarget(null);
      setResultNote("");
      applyUpdate(updated);
    } catch {
      toast.error("Cập nhật trạng thái thất bại");
    } finally {
      setDoneUpdating(false);
    }
  }

  const quickLabel: Record<string, string> = {
    CONFIRMED: "xác nhận",
    CANCELLED: "hủy",
  };

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Quản lý lịch hẹn</h1>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      ) : !result || result.content.length === 0 ? (
        <div className="flex items-center justify-center py-24 text-muted-foreground">
          <p className="text-base">Không có lịch hẹn nào</p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã</TableHead>
                  <TableHead>Khách</TableHead>
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
                    <TableCell className="font-mono font-medium">
                      #{appt.id.slice(-4).toUpperCase()}
                    </TableCell>
                    <TableCell className="font-mono text-muted-foreground">
                      #{appt.customerId.slice(-4).toUpperCase()}
                    </TableCell>
                    <TableCell className="max-w-32 truncate text-sm">
                      {appt.branchName}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
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
                      <DropdownMenu>
                        <DropdownMenuTrigger
                          render={
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              aria-label="Hành động"
                            />
                          }
                        >
                          <MoreHorizontal className="size-4" />
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {appt.status === "PENDING" && (
                            <DropdownMenuItem
                              onClick={() =>
                                setQuickTarget({ appt, status: "CONFIRMED" })
                              }
                            >
                              Xác nhận
                            </DropdownMenuItem>
                          )}
                          {(appt.status === "PENDING" ||
                            appt.status === "CONFIRMED") && (
                            <DropdownMenuItem
                              onClick={() => {
                                setResultNote("");
                                setDoneTarget(appt);
                              }}
                            >
                              Hoàn thành
                            </DropdownMenuItem>
                          )}
                          {appt.status !== "CANCELLED" &&
                            appt.status !== "DONE" && (
                              <DropdownMenuItem
                                onClick={() =>
                                  setQuickTarget({ appt, status: "CANCELLED" })
                                }
                                className="text-destructive focus:text-destructive"
                              >
                                Hủy
                              </DropdownMenuItem>
                            )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Trang {result.number + 1} / {result.totalPages} —{" "}
              {result.totalElements} lịch hẹn
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

      {/* Quick confirm / cancel dialog */}
      <Dialog
        open={!!quickTarget}
        onOpenChange={(open) => { if (!open) setQuickTarget(null); }}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>
              Xác nhận{" "}
              {quickTarget ? quickLabel[quickTarget.status] ?? quickTarget.status : ""}{" "}
              lịch hẹn
            </DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn có chắc muốn{" "}
            <span className="font-semibold text-foreground">
              {quickTarget ? quickLabel[quickTarget.status] ?? quickTarget.status : ""}
            </span>{" "}
            lịch hẹn{" "}
            <span className="font-semibold text-foreground">
              #{quickTarget?.appt.id.slice(-4).toUpperCase()}
            </span>
            ?
          </p>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setQuickTarget(null)}
              disabled={quickUpdating}
            >
              Không
            </Button>
            <Button
              variant={quickTarget?.status === "CANCELLED" ? "destructive" : "default"}
              onClick={handleQuickUpdate}
              disabled={quickUpdating}
            >
              {quickUpdating ? "Đang lưu..." : "Xác nhận"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Done dialog with resultNote */}
      <Dialog
        open={!!doneTarget}
        onOpenChange={(open) => { if (!open) { setDoneTarget(null); setResultNote(""); } }}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>
              Hoàn thành lịch hẹn #{doneTarget?.id.slice(-4).toUpperCase()}
            </DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="result-note">Kết quả khám (tùy chọn)</Label>
            <Textarea
              id="result-note"
              placeholder="Nhập kết quả hoặc ghi chú sau khám..."
              value={resultNote}
              onChange={(e) => setResultNote(e.target.value)}
              rows={4}
            />
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => { setDoneTarget(null); setResultNote(""); }}
              disabled={doneUpdating}
            >
              Hủy
            </Button>
            <Button onClick={handleDone} disabled={doneUpdating}>
              {doneUpdating ? "Đang lưu..." : "Hoàn thành"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
