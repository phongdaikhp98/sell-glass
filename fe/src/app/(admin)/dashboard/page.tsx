"use client";

import { useAuthStore } from "@/store/auth.store";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ShoppingCart, Package, Users, UserCog } from "lucide-react";

const SUMMARY_CARDS = [
  { label: "Đơn hàng", icon: ShoppingCart, value: "—" },
  { label: "Sản phẩm", icon: Package, value: "—" },
  { label: "Khách hàng", icon: Users, value: "—" },
  { label: "Nhân viên", icon: UserCog, value: "—" },
];

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);

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

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {SUMMARY_CARDS.map(({ label, icon: Icon, value }) => (
          <Card key={label}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
              <Icon className="size-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{value}</p>
              <p className="mt-1 text-xs text-muted-foreground">Tính năng đang phát triển</p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
