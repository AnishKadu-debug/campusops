import React from 'react';
import { Header } from './Header';

interface LayoutProps {
  children: React.ReactNode;
  onRefresh?: () => void;
  isRefreshing?: boolean;
}

export const Layout: React.FC<LayoutProps> = ({ children, onRefresh, isRefreshing }) => {
  return (
    <div className="min-h-screen bg-[#0c0e12] text-neutral-100 flex flex-col selection:bg-amber-500 selection:text-black">
      <Header onRefresh={onRefresh} isRefreshing={isRefreshing} />
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {children}
      </main>
      <footer className="border-t border-[#262c36] bg-[#0c0e12] py-3 text-center text-xs font-mono text-neutral-600">
        <div className="max-w-7xl mx-auto px-4 flex flex-wrap items-center justify-between gap-2">
          <div>
            CampusOps Learning &amp; Observability Depot &bull; Microservices Backend
          </div>
          <div>
            Spring Boot &bull; OpenFeign &bull; Eureka &bull; Kafka &bull; Postgres &bull; Mongo
          </div>
        </div>
      </footer>
    </div>
  );
};
