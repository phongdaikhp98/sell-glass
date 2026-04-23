"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  LayoutDashboard,
  ShoppingCart,
  Package,
  Tag,
  Bookmark,
  MapPin,
  Users,
  UserCog,
  CalendarClock,
  Ticket,
  LogOut,
  Menu,
} from "lucide-react";
import { useAuthStore } from "@/store/auth.store";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";

type AdminRole = "STAFF" | "BRANCH_MANAGER" | "SUPER_ADMIN";

const NAV_ITEMS: {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: AdminRole[];
}[] = [
  { href: "/admin/dashboard", label: "Dashboard",    icon: LayoutDashboard, roles: ["STAFF", "BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/orders",    label: "Đơn hàng",     icon: ShoppingCart,    roles: ["STAFF", "BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/appointments", label: "Lịch hẹn", icon: CalendarClock,   roles: ["STAFF", "BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/products",  label: "Sản phẩm",     icon: Package,         roles: ["BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/categories", label: "Danh mục",   icon: Tag,             roles: ["BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/brands",    label: "Thương hiệu",  icon: Bookmark,        roles: ["BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/branches",  label: "Chi nhánh",    icon: MapPin,          roles: ["SUPER_ADMIN"] },
  { href: "/admin/vouchers",  label: "Voucher",      icon: Ticket,          roles: ["BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/customers", label: "Khách hàng",   icon: Users,           roles: ["STAFF", "BRANCH_MANAGER", "SUPER_ADMIN"] },
  { href: "/admin/staff",     label: "Nhân viên",    icon: UserCog,         roles: ["SUPER_ADMIN"] },
];

function NavLinks({ pathname, role, onNavigate }: { pathname: string; role: string | null; onNavigate?: () => void }) {
  const visibleItems = NAV_ITEMS.filter((item) =>
    role ? item.roles.includes(role as AdminRole) : false
  );
  return (
    <nav className="flex flex-col gap-1">
      {visibleItems.map(({ href, label, icon: Icon }) => {
        const active = pathname === href || pathname.startsWith(href + "/");
        return (
          <Link
            key={href}
            href={href}
            onClick={onNavigate}
            className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
              active
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            }`}
          >
            <Icon className="size-4 shrink-0" />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const role = useAuthStore((s) => s.role);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const [checking, setChecking] = useState(true);
  const [sheetOpen, setSheetOpen] = useState(false);

  useEffect(() => {
    if (role === null) {
      router.replace("/login");
    } else if (role === "CUSTOMER") {
      router.replace("/login");
    } else {
      setChecking(false);
    }
  }, [role, router]);

  function handleLogout() {
    logout();
    router.replace("/login");
  }

  if (checking) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="flex flex-col gap-3 w-64">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-3/4" />
        </div>
      </div>
    );
  }

  const sidebarContent = (
    <div className="flex h-full flex-col">
      <div className="px-4 py-5">
        <span className="text-lg font-bold tracking-tight">Sell Glass Admin</span>
        {user && (
          <p className="mt-1 text-xs text-muted-foreground truncate">{user.fullName}</p>
        )}
      </div>
      <Separator />
      <div className="flex-1 overflow-y-auto px-3 py-4">
        <NavLinks pathname={pathname} role={role} onNavigate={() => setSheetOpen(false)} />
      </div>
      <Separator />
      <div className="px-3 py-4">
        <Button
          variant="ghost"
          className="w-full justify-start gap-3 text-muted-foreground hover:text-foreground"
          onClick={handleLogout}
        >
          <LogOut className="size-4 shrink-0" />
          Đăng xuất
        </Button>
      </div>
    </div>
  );

  return (
    <div className="flex min-h-screen">
      {/* Desktop sidebar */}
      <aside className="hidden w-60 shrink-0 border-r bg-background lg:flex lg:flex-col">
        {sidebarContent}
      </aside>

      {/* Mobile header + sheet */}
      <div className="flex flex-1 flex-col">
        <header className="flex h-14 items-center border-b px-4 lg:hidden">
          <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
            <SheetTrigger
              render={
                <Button variant="ghost" size="icon" aria-label="Mở menu" />
              }
            >
              <Menu className="size-5" />
            </SheetTrigger>
            <SheetContent side="left" className="w-60 p-0">
              <SheetHeader className="sr-only">
                <SheetTitle>Menu điều hướng</SheetTitle>
              </SheetHeader>
              {sidebarContent}
            </SheetContent>
          </Sheet>
          <span className="ml-3 text-base font-semibold">Sell Glass Admin</span>
        </header>

        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}
