import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Routes yêu cầu đăng nhập với role STAFF/ADMIN trở lên
const ADMIN_PREFIX = "/admin";

// Routes yêu cầu đăng nhập với role CUSTOMER
const CUSTOMER_PATHS = ["/orders", "/profile", "/appointments", "/cart/checkout"];

// Routes chỉ dành cho người CHƯA đăng nhập (redirect về / nếu đã đăng nhập)
const GUEST_ONLY_PATHS = ["/login", "/register", "/forgot-password", "/reset-password"];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Cookie flag được set bởi auth.store.ts khi setTokens() / logout()
  const hasSession = request.cookies.has("sg_session");

  const isAdminPath = pathname.startsWith(ADMIN_PREFIX);
  const isCustomerPath = CUSTOMER_PATHS.some(
    (p) => pathname === p || pathname.startsWith(p + "/")
  );
  const isGuestOnlyPath = GUEST_ONLY_PATHS.some(
    (p) => pathname === p || pathname.startsWith(p + "/")
  );

  // Chặn truy cập admin/customer routes khi chưa có session
  if ((isAdminPath || isCustomerPath) && !hasSession) {
    const loginUrl = new URL("/login", request.url);
    return NextResponse.redirect(loginUrl);
  }

  // Redirect về trang chủ nếu đã đăng nhập mà vào trang guest-only
  if (isGuestOnlyPath && hasSession) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match tất cả paths NGOẠI TRỪ:
     * - _next/static, _next/image (Next.js internals)
     * - favicon.ico, các file public tĩnh
     * - api routes
     */
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico|woff2?|ttf|otf|eot)).*)",
  ],
};
