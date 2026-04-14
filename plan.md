# sell-glass — Project Plan

## Stack
| Layer | Công nghệ | Hosting |
|---|---|---|
| Frontend | Next.js | Vercel Free |
| Backend | Java Spring Boot (Gradle) | Oracle Cloud ARM VM (free) |
| Database | PostgreSQL | Tự host trên Oracle VM |
| Cache | Redis | Tự host trên Oracle VM |
| Image | Cloudinary | Free tier 25GB |
| Domain | .vn / .com | ~$1/tháng |

**Tổng chi phí: ~$1/tháng**

## Repo
```
sell-glass-be/   # Spring Boot
sell-glass-fe/   # Next.js
```

## Actors
| Actor | Mô tả |
|---|---|
| Guest | Khách chưa đăng nhập — xem sản phẩm, tìm kiếm |
| Customer | Khách đã đăng nhập — đặt hàng, đặt lịch, xem lịch sử đơn |
| Staff | Nhân viên chi nhánh — xử lý đơn, quản lý khách |
| Branch Manager | Quản lý chi nhánh — tất cả của Staff + quản lý nhân viên chi nhánh |
| Super Admin | Toàn quyền — tất cả chi nhánh, sản phẩm, báo cáo |

## Modules

### Phase 1 — MVP
| # | Module | Actor |
|---|---|---|
| 1 | AUTH — Đăng ký/đăng nhập, JWT + Refresh Token, phân quyền role + branch | All |
| 2 | CATALOG — Danh sách, tìm kiếm, lọc, chi tiết sản phẩm + biến thể | Guest, Customer |
| 3 | BRANCHES — Danh sách chi nhánh, địa chỉ, giờ, SĐT | Guest, Customer |
| 4 | CART & ORDER — Giỏ hàng, đặt hàng (Pickup/Delivery), QR thanh toán, upload bill, theo dõi đơn | Customer |
| 5 | PRODUCT MANAGEMENT — CRUD sản phẩm, biến thể, danh mục, thương hiệu, ảnh Cloudinary | Admin, Manager |
| 6 | ORDER MANAGEMENT — Danh sách đơn, cập nhật trạng thái, xác nhận thanh toán | Admin, Manager, Staff |
| 7 | CUSTOMER MANAGEMENT — Danh sách khách, chi tiết, lịch sử đơn | Admin, Manager, Staff |
| 8 | STAFF MANAGEMENT — CRUD nhân viên, phân role, gán chi nhánh | Admin, Manager |
| 9 | NOTIFICATION — Email: xác nhận đơn, cập nhật trạng thái, đơn mới (SMTP) | System |

### Phase 2 — Hoàn thiện
| # | Module | Actor |
|---|---|---|
| 10 | APPOINTMENT — Đặt lịch thử kính/khám mắt, chọn chi nhánh + giờ | Customer |
| 11 | APPOINTMENT MANAGEMENT — Xác nhận/từ chối lịch, ghi chú kết quả | Staff, Manager |
| 12 | REPORTS — Doanh thu theo chi nhánh, đơn theo trạng thái, bán chạy | Admin, Manager |
| 13 | ORDER PDF — In phiếu đơn hàng | Admin, Manager, Staff |

### Phase 3 — Nice to have
- Zalo OA notification
- Loyalty / điểm tích lũy
- Virtual try-on (thử kính ảo)

## Trạng thái đơn hàng
```
Pending → Confirmed → Processing → Ready → Delivering → Completed
                                                       ↘ Cancelled (từ bất kỳ bước nào)
```

## Những thứ KHÔNG làm
- Tích hợp VNPAY / MoMo (dùng QR chuyển khoản)
- Toa kính / prescription (chưa cần)
- Quản lý tồn kho / chuyển hàng giữa chi nhánh
- App mobile riêng (responsive web)
- Live chat
- Flash sale / coupon phức tạp
- Spring Native / GraalVM (Oracle VM có 24GB RAM, không cần)
- Monorepo (Java + Next.js khác hệ sinh thái)
