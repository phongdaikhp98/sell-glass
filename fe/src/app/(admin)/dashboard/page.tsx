"use client";

import { useEffect, useState } from "react";
import { useAuthStore } from "@/store/auth.store";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ShoppingCart, Package, Users, TrendingUp } from "lucide-react";
import {
  getSummary,
  getRevenue,
  getTopProducts,
  getOrdersByStatus,
  type SummaryReport,
} from "@/lib/report.api";

const formatVND = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PROCESSING: "Đang xử lý",
  READY: "Sẵn sàng",
  DELIVERING: "Đang giao",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const ORDER_STATUS_CLASS: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800 border-yellow-200",
  CONFIRMED: "bg-blue-100 text-blue-800 border-blue-200",
  PROCESSING: "bg-indigo-100 text-indigo-800 border-indigo-200",
  READY: "bg-purple-100 text-purple-800 border-purple-200",
  DELIVERING: "bg-orange-100 text-orange-800 border-orange-200",
  COMPLETED: "bg-green-100 text-green-800 border-green-200",
  CANCELLED: "bg-red-100 text-red-800 border-red-200",
};

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  const [summary, setSummary] = useState<SummaryReport | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(true);

  const [revenue, setRevenue] = useState<{ date: string; revenue: number }[]>([]);
  const [revenueLoading, setRevenueLoading] = useState(true);

  const [topProducts, setTopProducts] = useState<
    { productName: string; totalQuantity: number; totalRevenue: number }[]
  >([]);
  const [topProductsLoading, setTopProductsLoading] = useState(true);

  const [ordersByStatus, setOrdersByStatus] = useState<{ status: string; count: number }[]>([]);
  const [ordersByStatusLoading, setOrdersByStatusLoading] = useState(true);

  useEffect(() => {
    getSummary()
      .then(setSummary)
      .catch(() => {})
      .finally(() => setSummaryLoading(false));

    getRevenue("month")
      .then(setRevenue)
      .catch(() => {})
      .finally(() => setRevenueLoading(false));

    getTopProducts(5)
      .then(setTopProducts)
      .catch(() => {})
      .finally(() => setTopProductsLoading(false));

    getOrdersByStatus()
      .then(setOrdersByStatus)
      .catch(() => {})
      .finally(() => setOrdersByStatusLoading(false));
  }, []);

  const maxRevenue = revenue.length > 0 ? Math.max(...revenue.map((r) => r.revenue)) : 0;

  const summaryCards = [
    {
      label: "Đơn hàng",
      icon: ShoppingCart,
      value: summary ? summary.totalOrders.toLocaleString("vi-VN") : "—",
    },
    {
      label: "Doanh thu",
      icon: TrendingUp,
      value: summary ? formatVND(summary.totalRevenue) : "—",
    },
    {
      label: "Khách hàng",
      icon: Users,
      value: summary ? summary.totalCustomers.toLocaleString("vi-VN") : "—",
    },
    {
      label: "Sản phẩm",
      icon: Package,
      value: summary ? summary.totalProducts.toLocaleString("vi-VN") : "—",
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>Chào mừng trở lại{user ? `, ${user.fullName}` : ""}!</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Đây là trang quản trị hệ thống Sell Glass. Sử dụng thanh điều hướng bên trái để quản lý dữ liệu.
          </p>
        </CardContent>
      </Card>

      {/* Summary cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {summaryCards.map(({ label, icon: Icon, value }) => (
          <Card key={label}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
              <Icon className="size-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {summaryLoading ? (
                <Skeleton className="h-7 w-28" />
              ) : (
                <p className="text-2xl font-bold">{value}</p>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Orders by status */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Đơn hàng theo trạng thái</CardTitle>
        </CardHeader>
        <CardContent>
          {ordersByStatusLoading ? (
            <div className="flex flex-wrap gap-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-8 w-32 rounded-full" />
              ))}
            </div>
          ) : ordersByStatus.length === 0 ? (
            <p className="text-sm text-muted-foreground">Chưa có dữ liệu</p>
          ) : (
            <div className="flex flex-wrap gap-3">
              {ordersByStatus.map(({ status, count }) => (
                <div
                  key={status}
                  className={`flex items-center gap-2 rounded-full border px-3 py-1 text-sm font-medium ${ORDER_STATUS_CLASS[status] ?? "bg-muted text-muted-foreground"}`}
                >
                  <span>{ORDER_STATUS_LABEL[status] ?? status}</span>
                  <span className="font-bold">{count}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Revenue by month */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Doanh thu theo tháng</CardTitle>
          </CardHeader>
          <CardContent>
            {revenueLoading ? (
              <div className="flex flex-col gap-3">
                {Array.from({ length: 6 }).map((_, i) => (
                  <Skeleton key={i} className="h-8 w-full" />
                ))}
              </div>
            ) : revenue.length === 0 ? (
              <p className="text-sm text-muted-foreground">Chưa có dữ liệu</p>
            ) : (
              <div className="flex flex-col gap-2">
                {revenue.map(({ date, revenue: rev }) => {
                  const pct = maxRevenue > 0 ? Math.round((rev / maxRevenue) * 100) : 0;
                  return (
                    <div key={date} className="flex items-center gap-3">
                      <span className="w-20 shrink-0 text-right text-xs text-muted-foreground">
                        {date}
                      </span>
                      <div className="flex-1 rounded-full bg-muted overflow-hidden h-5">
                        <div
                          className="h-full rounded-full bg-primary transition-all"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <span className="w-28 shrink-0 text-xs font-medium">
                        {formatVND(rev)}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Top products */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Sản phẩm bán chạy</CardTitle>
          </CardHeader>
          <CardContent>
            {topProductsLoading ? (
              <div className="flex flex-col gap-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-8 w-full" />
                ))}
              </div>
            ) : topProducts.length === 0 ? (
              <p className="text-sm text-muted-foreground">Chưa có dữ liệu</p>
            ) : (
              <div className="rounded-md border overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b bg-muted/50">
                      <th className="px-3 py-2 text-left font-medium text-muted-foreground">Sản phẩm</th>
                      <th className="px-3 py-2 text-right font-medium text-muted-foreground">Số lượng</th>
                      <th className="px-3 py-2 text-right font-medium text-muted-foreground">Doanh thu</th>
                    </tr>
                  </thead>
                  <tbody>
                    {topProducts.map(({ productName, totalQuantity, totalRevenue }, idx) => (
                      <tr
                        key={productName}
                        className={idx < topProducts.length - 1 ? "border-b" : ""}
                      >
                        <td className="px-3 py-2 font-medium">{productName}</td>
                        <td className="px-3 py-2 text-right text-muted-foreground">
                          {totalQuantity.toLocaleString("vi-VN")}
                        </td>
                        <td className="px-3 py-2 text-right font-medium">
                          {formatVND(totalRevenue)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
