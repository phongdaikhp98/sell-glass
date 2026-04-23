import type { Metadata } from "next";
import ProductDetailClient from "./ProductDetailClient";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function generateMetadata(
  { params }: { params: Promise<{ slug: string }> }
): Promise<Metadata> {
  const { slug } = await params;
  try {
    const res = await fetch(`${API_URL}/v1/products/${slug}`, {
      next: { revalidate: 3600 },
    });
    if (!res.ok) return { title: "Sản phẩm" };

    const json = await res.json();
    const product = json.data;
    if (!product) return { title: "Sản phẩm" };

    const title = product.name as string;
    const brandName: string = product.brandName ?? product.brand?.name ?? "";
    const description: string = product.description
      ? (product.description as string).slice(0, 160)
      : `${title}${brandName ? ` — ${brandName}` : ""} tại Sell Glass`;

    const image: string | undefined =
      product.primaryImageUrl ??
      (Array.isArray(product.images) && product.images.length > 0
        ? product.images[0].url
        : undefined);

    return {
      title,
      description,
      openGraph: {
        title,
        description,
        url: `${SITE_URL}/products/${slug}`,
        type: "website",
        images: image ? [{ url: image, width: 800, height: 800, alt: title }] : [],
      },
    };
  } catch {
    return { title: "Sản phẩm" };
  }
}

export default async function ProductDetailPage(
  { params }: { params: Promise<{ slug: string }> }
) {
  const { slug } = await params;
  return <ProductDetailClient slug={slug} />;
}
