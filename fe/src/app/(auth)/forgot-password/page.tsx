"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { toast } from "sonner";

import { forgotPassword } from "@/lib/auth.api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const schema = z.object({
  email: z.string().email("Email không hợp lệ"),
});

type FormValues = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  async function onSubmit(values: FormValues) {
    try {
      await forgotPassword(values.email);
    } catch {
      // BE luôn trả 200 — nếu lỗi network thì vẫn thông báo như thành công
    } finally {
      toast.success(
        "Nếu email tồn tại, link đặt lại đã được gửi. Vui lòng kiểm tra hộp thư."
      );
      setSubmitted(true);
    }
  }

  if (submitted) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Kiểm tra hộp thư</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Nếu email của bạn tồn tại trong hệ thống, chúng tôi đã gửi link đặt
            lại mật khẩu. Vui lòng kiểm tra hộp thư (kể cả thư mục spam).
          </p>
        </CardContent>
        <CardFooter>
          <Link
            href="/login"
            className="text-sm text-muted-foreground underline-offset-4 hover:underline"
          >
            ← Quay lại đăng nhập
          </Link>
        </CardFooter>
      </Card>
    );
  }

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Quên mật khẩu</CardTitle>
        <p className="text-sm text-muted-foreground">
          Nhập email đã đăng ký để nhận link đặt lại mật khẩu
        </p>
      </CardHeader>

      <CardContent>
        <form id="forgot-form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              aria-invalid={!!errors.email}
              {...register("email")}
            />
            {errors.email && (
              <p className="text-xs text-destructive">{errors.email.message}</p>
            )}
          </div>
        </form>
      </CardContent>

      <CardFooter className="flex flex-col gap-3">
        <Button
          type="submit"
          form="forgot-form"
          className="w-full"
          disabled={isSubmitting}
        >
          {isSubmitting ? "Đang gửi..." : "Gửi link đặt lại"}
        </Button>

        <Link
          href="/login"
          className="text-sm text-muted-foreground underline-offset-4 hover:underline"
        >
          ← Quay lại đăng nhập
        </Link>
      </CardFooter>
    </Card>
  );
}
