import type { Metadata } from "next";
import Header from "@/components/layout/Header";

export const metadata: Metadata = {
  openGraph: {
    images: [{ url: "/og-default.jpg", width: 1200, height: 630, alt: "Sell Glass" }],
  },
};

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1">{children}</main>
      <footer className="border-t py-6 text-center text-sm text-muted-foreground">
        &copy; {new Date().getFullYear()} Sell Glass. Bảo lưu mọi quyền.
      </footer>
    </div>
  );
}
