/** @type {import('next').NextConfig} */
const nextConfig = {
  turbopack: {
    // Use absolute path for turbopack.root to avoid workspace root detection warnings
    root: '/Users/vishrutharavi/Documents/wecureit/frontend',
  },
};

module.exports = nextConfig;