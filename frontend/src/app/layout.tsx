import "./globals.css";

export const metadata = {
  title: "WeCureIT",
  description: "Medical appointment booking made easy",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <div className="app-root">
          <main className="app-content">{children}</main>
          <footer className="site-footer">© 2025 WeCureIT. All rights reserved.</footer>
        </div>
      </body>
    </html>
  );
}
