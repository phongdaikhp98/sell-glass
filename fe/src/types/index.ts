// Auth
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// Branch
export interface Branch {
  id: string;
  name: string;
  address: string;
  phone: string;
  openTime: string;
  closeTime: string;
  isActive: boolean;
}

// Catalog
export interface Category {
  id: string;
  name: string;
  slug: string;
}

export interface Brand {
  id: string;
  name: string;
  slug: string;
  logoUrl?: string;
}

export interface ProductVariant {
  id: string;
  sku: string;
  color: string;
  size: string;
  price: number;
  isActive: boolean;
  stock: number;
}

export interface ProductListItem {
  id: string;
  name: string;
  slug: string;
  category: Category;
  brand: Brand;
  primaryImageUrl?: string;
  minPrice: number;
  maxPrice: number;
  gender: "MEN" | "WOMEN" | "UNISEX";
}

export interface Product extends ProductListItem {
  description: string;
  frameShape: string;
  material: string;
  images: { id: string; url: string; sortOrder: number; isPrimary: boolean }[];
  variants: ProductVariant[];
}

// Cart
export interface CartItem {
  id: string;
  variant: ProductVariant & { productName: string; primaryImageUrl?: string };
  quantity: number;
}

export interface Cart {
  id: string;
  items: CartItem[];
  total: number;
}

// Order
export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "READY"
  | "DELIVERING"
  | "COMPLETED"
  | "CANCELLED";

export type PaymentStatus = "UNPAID" | "PENDING_VERIFY" | "PAID";

export interface Prescription {
  odSph: number | null;
  odCyl: number | null;
  odAxis: number | null;
  osSph: number | null;
  osCyl: number | null;
  osAxis: number | null;
  pd: number | null;
  note: string | null;
}

export interface Order {
  id: string;
  orderType: "PICKUP" | "DELIVERY";
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  total: number;
  voucherCode?: string | null;
  branch: Pick<Branch, "id" | "name" | "address">;
  items: {
    id: string;
    productName: string;
    variantSku: string;
    unitPrice: number;
    quantity: number;
    subtotal: number;
  }[];
  prescription?: Prescription | null;
  createdAt: string;
}

export interface Voucher {
  id: string;
  code: string;
  type: "PERCENTAGE" | "FIXED_AMOUNT";
  value: number;
  maxDiscountAmount?: number | null;
  minOrderAmount: number;
  usageLimit?: number | null;
  timesUsed: number;
  expiresAt?: string | null;
  isActive: boolean;
  createdAt: string;
}

// Review
export interface Review {
  id: string;
  customerId: string;
  customerName: string;
  rating: number;
  comment?: string | null;
  createdAt: string;
}

// Appointment
export type AppointmentStatus = "PENDING" | "CONFIRMED" | "DONE" | "CANCELLED";

export interface Appointment {
  id: string;
  branch: Pick<Branch, "id" | "name" | "address">;
  scheduledAt: string;
  status: AppointmentStatus;
  note?: string;
  resultNote?: string;
}

// Customer
export interface Customer {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
}

// API Response
export interface ApiResponse<T> {
  code: string;
  message: string;
  requestId: string;
  timestamp: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
