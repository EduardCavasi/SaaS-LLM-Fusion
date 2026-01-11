import type { Metadata } from "next";
import "./globals.css";
import Sidebar from "@/components/Sidebar";
import VerificationBanner from "@/components/VerificationBanner";

export const metadata: Metadata = {
  title: "Verified Meeting Scheduler",
  description: "Meeting scheduler with Z3 constraint solving and runtime verification",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased">
        <div className="flex min-h-screen">
          <Sidebar />
          <div className="flex-1 flex flex-col">
            <VerificationBanner />
            <main className="flex-1 p-6 overflow-auto">
              {children}
            </main>
          </div>
        </div>
      </body>
    </html>
  );
}
