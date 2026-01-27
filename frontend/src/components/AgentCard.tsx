import { useState, useRef, useEffect, useMemo } from 'react';
import MetricsChart, { MultiLineChart } from './MetricsChart';
import type { RealtimeMetrics, MetricsHistoryPoint, DiskInfo, NetworkInterface } from '../types/agent';
import type { Agent } from '../types/agent';

interface AgentCardProps {
  agent: Agent;
  metrics: RealtimeMetrics | undefined;
  history: MetricsHistoryPoint[];
  onDelete: (id: number) => void;
}

function formatBytes(bytes: number | null): string {
  if (bytes === null || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatBytesPerSec(bytes: number | null): string {
  if (bytes === null || bytes === 0) return '0 B/s';
  const k = 1024;
  const sizes = ['B/s', 'KB/s', 'MB/s', 'GB/s'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatUptime(seconds: number | null): string {
  if (seconds === null) return '-';
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (days > 0) return `${days}d ${hours}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

function getUsageColor(usage: number): string {
  if (usage >= 90) return 'text-red-400';
  if (usage >= 70) return 'text-yellow-400';
  return 'text-emerald-400';
}

function getUsageBarColor(usage: number): string {
  if (usage >= 90) return 'bg-red-500';
  if (usage >= 70) return 'bg-yellow-500';
  return 'bg-emerald-500';
}

function MiniGauge({ value, label, subLabel }: { value: number; label: string; subLabel?: string }) {
  const color = getUsageColor(value);
  const barColor = getUsageBarColor(value);
  
  return (
    <div className="flex flex-col">
      <div className="flex items-baseline justify-between mb-1">
        <span className="text-xs text-slate-400">{label}</span>
        <span className={`text-sm font-bold ${color} tabular-nums`}>
          {value.toFixed(1)}%
        </span>
      </div>
      <div className="w-full bg-slate-700 rounded-full h-1.5">
        <div
          className={`h-1.5 rounded-full transition-all duration-300 ${barColor}`}
          style={{ width: `${Math.min(value, 100)}%` }}
        />
      </div>
      {subLabel && (
        <span className="text-[10px] text-slate-500 mt-0.5">{subLabel}</span>
      )}
    </div>
  );
}

function DiskCard({ disk }: { disk: DiskInfo }) {
  return (
    <div className="bg-slate-700/30 rounded-lg p-3">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs text-slate-400 truncate max-w-[120px]" title={disk.mountpoint}>
          {disk.mountpoint}
        </span>
        <span className={`text-sm font-semibold ${getUsageColor(disk.usage)}`}>
          {disk.usage.toFixed(1)}%
        </span>
      </div>
      <div className="w-full bg-slate-700 rounded-full h-1.5 mb-2">
        <div
          className={`h-1.5 rounded-full transition-all duration-300 ${getUsageBarColor(disk.usage)}`}
          style={{ width: `${Math.min(disk.usage, 100)}%` }}
        />
      </div>
      <div className="flex justify-between text-[10px] text-slate-500">
        <span>{formatBytes(disk.usedBytes)}</span>
        <span>{formatBytes(disk.totalBytes)}</span>
      </div>
    </div>
  );
}

function NetworkCard({ iface }: { iface: NetworkInterface }) {
  const isLoopback = iface.name === 'lo' || iface.name.startsWith('lo');
  if (isLoopback) return null;

  return (
    <div className="bg-slate-700/30 rounded-lg p-3">
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm font-medium text-slate-300">{iface.name}</span>
        {iface.ips && iface.ips.length > 0 && (
          <span className="text-[10px] text-slate-500 truncate max-w-[100px]" title={iface.ips[0]}>
            {iface.ips[0].split('/')[0]}
          </span>
        )}
      </div>
      <div className="grid grid-cols-2 gap-2">
        <div>
          <p className="text-[10px] text-slate-400">IN</p>
          <p className="text-sm font-semibold text-emerald-400 tabular-nums">
            {formatBytesPerSec(iface.recvRate)}
          </p>
        </div>
        <div>
          <p className="text-[10px] text-slate-400">OUT</p>
          <p className="text-sm font-semibold text-orange-400 tabular-nums">
            {formatBytesPerSec(iface.sentRate)}
          </p>
        </div>
      </div>
    </div>
  );
}

export default function AgentCard({ agent, metrics, history, onDelete }: AgentCardProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState<'charts' | 'disks' | 'network'>('charts');
  const isOnline = metrics?.online ?? false;

  const prevInterfacesRef = useRef<Map<string, NetworkInterface>>(new Map());

  useEffect(() => {
    if (metrics?.interfaces) {
      metrics.interfaces.forEach((iface) => {
        if (iface.recvRate !== null && iface.recvRate !== undefined) {
          prevInterfacesRef.current.set(iface.name, iface);
        }
      });
    }
  }, [metrics?.interfaces]);

  const getInterfaceWithFallback = (iface: NetworkInterface): NetworkInterface => {
    const prev = prevInterfacesRef.current.get(iface.name);
    return {
      ...iface,
      recvRate: iface.recvRate ?? prev?.recvRate ?? 0,
      sentRate: iface.sentRate ?? prev?.sentRate ?? 0,
    };
  };

  const totalNetworkRx = metrics?.interfaces?.reduce((sum, i) => sum + (i.recvRate || 0), 0) ?? 0;
  const totalNetworkTx = metrics?.interfaces?.reduce((sum, i) => sum + (i.sentRate || 0), 0) ?? 0;

  const filteredInterfaces = useMemo(() => {
    return metrics?.interfaces?.filter(
      (i) => i.name !== 'lo' && !i.name.startsWith('lo')
    ).map(getInterfaceWithFallback) ?? [];
  }, [metrics?.interfaces]);

  const totalDiskUsed = metrics?.disks?.reduce((sum, d) => sum + d.usedBytes, 0) ?? 0;
  const totalDiskTotal = metrics?.disks?.reduce((sum, d) => sum + d.totalBytes, 0) ?? 0;
  const avgDiskUsage = totalDiskTotal > 0 ? (totalDiskUsed / totalDiskTotal) * 100 : 0;

  return (
    <div className="bg-slate-800/80 backdrop-blur rounded-xl border border-slate-700/50 overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700/50 bg-slate-800">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <span
              className={`w-2 h-2 rounded-full ${isOnline ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`}
            />
            <h3 className="text-base font-semibold text-white">{agent.name}</h3>
          </div>
          {metrics?.os && (
            <span className="text-xs text-slate-500 hidden sm:inline">
              {metrics.os} {metrics.platform && `(${metrics.platform})`}
            </span>
          )}
          <span className="text-xs text-slate-500 font-mono">
            {agent.hostname || agent.nodeId}
          </span>
        </div>
        <div className="flex items-center gap-2">
          {isOnline && (
            <button
              onClick={() => setIsCollapsed(!isCollapsed)}
              className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-700 rounded transition-colors"
              title={isCollapsed ? 'Expand' : 'Collapse'}
            >
              <svg
                className={`w-4 h-4 transition-transform ${isCollapsed ? 'rotate-180' : ''}`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>
          )}
          <button
            onClick={() => onDelete(agent.id)}
            className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-slate-700 rounded transition-colors"
            title="Delete"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      {isOnline && metrics && !isCollapsed && (
        <>
          {/* Quick Stats Bar */}
          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-3 px-4 py-3 bg-slate-800/50 border-b border-slate-700/30">
            <MiniGauge
              value={metrics.cpuUsage ?? 0}
              label="CPU"
            />
            <MiniGauge
              value={metrics.memoryUsage ?? 0}
              label="Memory"
              subLabel={`${formatBytes(metrics.memoryUsed)} / ${formatBytes(metrics.memoryTotal)}`}
            />
            <MiniGauge
              value={avgDiskUsage}
              label={`Disk${metrics.disks && metrics.disks.length > 1 ? ` (${metrics.disks.length})` : ''}`}
              subLabel={`${formatBytes(totalDiskUsed)} / ${formatBytes(totalDiskTotal)}`}
            />
            <div className="flex flex-col">
              <span className="text-xs text-slate-400 mb-1">Network</span>
              <div className="flex items-center gap-2">
                <span className="text-xs text-emerald-400 tabular-nums">↓{formatBytesPerSec(totalNetworkRx)}</span>
                <span className="text-xs text-orange-400 tabular-nums">↑{formatBytesPerSec(totalNetworkTx)}</span>
              </div>
            </div>
            {metrics.temperature !== null && metrics.temperature !== undefined && metrics.temperature > 0 && (
              <div className="flex flex-col">
                <span className="text-xs text-slate-400 mb-1">Temp</span>
                <span className={`text-sm font-bold tabular-nums ${
                  metrics.temperature > 80 ? 'text-red-400' :
                  metrics.temperature > 60 ? 'text-yellow-400' : 'text-emerald-400'
                }`}>
                  {metrics.temperature}°C
                </span>
              </div>
            )}
            <div className="flex flex-col">
              <span className="text-xs text-slate-400 mb-1">Uptime</span>
              <span className="text-sm font-bold text-slate-200 tabular-nums">
                {formatUptime(metrics.uptimeSeconds)}
              </span>
              {metrics.processCount !== null && (
                <span className="text-[10px] text-slate-500">{metrics.processCount} procs</span>
              )}
            </div>
          </div>

          {/* Tabs */}
          <div className="flex gap-1 px-4 pt-3 bg-slate-900/30">
            <button
              onClick={() => setActiveTab('charts')}
              className={`px-3 py-1.5 text-xs font-medium rounded-t-lg transition-colors ${
                activeTab === 'charts'
                  ? 'bg-slate-700/50 text-white border-t border-x border-slate-600/50'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Charts
            </button>
            <button
              onClick={() => setActiveTab('disks')}
              className={`px-3 py-1.5 text-xs font-medium rounded-t-lg transition-colors ${
                activeTab === 'disks'
                  ? 'bg-slate-700/50 text-white border-t border-x border-slate-600/50'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Disks ({metrics.disks?.length ?? 0})
            </button>
            <button
              onClick={() => setActiveTab('network')}
              className={`px-3 py-1.5 text-xs font-medium rounded-t-lg transition-colors ${
                activeTab === 'network'
                  ? 'bg-slate-700/50 text-white border-t border-x border-slate-600/50'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Network ({filteredInterfaces.length})
            </button>
          </div>

          {/* Tab Content */}
          <div className="p-4 bg-slate-900/30">
            {activeTab === 'charts' && (
              <>
                {history.length > 1 ? (
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
                    <MetricsChart
                      data={history}
                      dataKey="cpuUsage"
                      title="CPU Usage"
                      color="#10b981"
                      unit="%"
                      maxY={100}
                      height={140}
                    />
                    <MetricsChart
                      data={history}
                      dataKey="memoryUsage"
                      title="Memory Usage"
                      color="#8b5cf6"
                      unit="%"
                      maxY={100}
                      height={140}
                    />
                    <MetricsChart
                      data={history}
                      dataKey="diskUsage"
                      title="Disk Usage"
                      color="#f59e0b"
                      unit="%"
                      maxY={100}
                      height={140}
                    />
                    <MultiLineChart
                      data={history}
                      lines={[
                        { dataKey: 'networkRxRate', color: '#10b981', name: 'IN' },
                        { dataKey: 'networkTxRate', color: '#f97316', name: 'OUT' },
                      ]}
                      title="Network I/O"
                      unit="bytes"
                      height={140}
                    />
                    {history.some((h) => h.temperature !== null && h.temperature > 0) && (
                      <MetricsChart
                        data={history}
                        dataKey="temperature"
                        title="Temperature"
                        color="#ef4444"
                        unit="°C"
                        height={140}
                      />
                    )}
                  </div>
                ) : (
                  <div className="flex items-center justify-center h-32 text-slate-500 text-sm">
                    <div className="flex items-center gap-2">
                      <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                      </svg>
                      Collecting data...
                    </div>
                  </div>
                )}
              </>
            )}

            {activeTab === 'disks' && (
              <div>
                {metrics.disks && metrics.disks.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                    {metrics.disks.map((disk, index) => (
                      <DiskCard key={`${disk.device}-${index}`} disk={disk} />
                    ))}
                  </div>
                ) : (
                  <div className="text-center text-slate-500 text-sm py-8">
                    No disk information available.
                  </div>
                )}
              </div>
            )}

            {activeTab === 'network' && (
              <div>
                {filteredInterfaces.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                    {filteredInterfaces.map((iface, index) => (
                      <NetworkCard key={`${iface.name}-${index}`} iface={iface} />
                    ))}
                  </div>
                ) : (
                  <div className="text-center text-slate-500 text-sm py-8">
                    No network interface information available.
                  </div>
                )}
              </div>
            )}
          </div>
        </>
      )}

      {isOnline && metrics && isCollapsed && (
        <div className="px-4 py-2 bg-slate-800/50 flex items-center gap-4 text-xs">
          <span className={getUsageColor(metrics.cpuUsage ?? 0)}>
            CPU: {metrics.cpuUsage?.toFixed(1)}%
          </span>
          <span className={getUsageColor(metrics.memoryUsage ?? 0)}>
            Mem: {metrics.memoryUsage?.toFixed(1)}%
          </span>
          <span className="text-emerald-400">↓{formatBytesPerSec(totalNetworkRx)}</span>
          <span className="text-orange-400">↑{formatBytesPerSec(totalNetworkTx)}</span>
        </div>
      )}

      {!isOnline && (
        <div className="px-4 py-6 text-center text-slate-500 text-sm">
          Agent offline. Waiting for connection...
        </div>
      )}
    </div>
  );
}
