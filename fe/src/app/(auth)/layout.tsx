import Link from "next/link";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-muted/40 px-4">
      <div className="mb-8 text-center">
        <Link href="/" className="inline-block">
          <span className="text-2xl font-bold tracking-tight">SellGlass</span>
        </Link>
        <p className="mt-1 text-sm text-muted-foreground">
          Cửa hàng kính mắt trực tuyến
        </p>
      </div>
      {children}
    </div>
  );
}
