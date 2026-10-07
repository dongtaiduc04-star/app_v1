import type { Metadata } from "next";
import { LanguageProvider } from "@/components/language-provider";
import "./globals.css";

export const metadata: Metadata = {
  title: "getlink_dtd — Mọi liên kết của bạn",
  description: "Tạo một trang gọn đẹp cho mọi liên kết quan trọng của bạn.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return <html lang="vi"><body><LanguageProvider>{children}</LanguageProvider></body></html>;
}
