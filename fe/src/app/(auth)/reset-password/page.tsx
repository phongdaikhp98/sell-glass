"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";

import { resetPassword } from "@/lib/auth.api";
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

const schema = z
  .object({
    newPassword: z.string().min(6, "Mật khẩu tối thiểu 6 ký tự"),
    confirm: z.string(),
  })
  .refine((d) => d.newPassword === d.confirm, {
    path: ["confirm"],
    message: "Mật khẩu xác nhận không khớp",
  });

type FormValues = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  if (!token) {
    return (
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Link không hợp lệ</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.
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

  async function onSubmit(values: FormValues) {
    try {
      await resetPassword(token as string, values.newPassword);
      toast.success("Đặt lại mật khẩu thành công");
      router.push("/login");
    } catch (err: unknown) {
      const apiMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message;
      toast.error(apiMessage ?? "Đặt lại mật khẩu thất bại");
    }
  }

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle>Đặt lại mật khẩu</CardTitle>
      </CardHeader>

      <CardContent>
        <form id="reset-form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="newPassword">Mật khẩu mới</Label>
              <Input
                id="newPassword"
                type="password"
                placeholder="••••••"
                autoComplete="new-password"
                aria-invalid={!!errors.newPassword}
                {...register("newPassword")}
              />
              {errors.newPassword && (
                <p className="text-xs text-destructive">
                  {errors.newPassword.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="confirm">Xác nhận mật khẩu mới</Label>
              <Input
                id="confirm"
                type="password"
                placeholder="••••••"
                autoComplete="new-password"
                aria-invalid={!!errors.confirm}
                {...register("confirm")}
              />
              {errors.confirm && (
                <p className="text-xs text-destructive">
                  {errors.confirm.message}
                </p>
              )}
            </div>
          </div>
        </form>
      </CardContent>

      <CardFooter className="flex flex-col gap-3">
        <Button
          type="submit"
          form="reset-form"
          className="w-full"
          disabled={isSubmitting}
        >
          {isSubmitting ? "Đang đặt lại..." : "Đặt lại mật khẩu"}
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
