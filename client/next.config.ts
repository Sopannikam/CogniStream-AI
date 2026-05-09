import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Keep builds reproducible on machines with limited resources.
  experimental: { cpus: 1 },
};

export default nextConfig;
