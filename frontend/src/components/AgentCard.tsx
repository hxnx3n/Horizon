import { useState } from 'react';
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
  return 'text-white';
}

function getUsageBarColor(usage: number): string {
  if (usage >= 90) return 'bg-red-500';
  if (usage >= 70) return 'bg-yellow-500';
  return 'bg-blue-500';
}

function DiskCard({ disk }: { disk: DiskInfo }) {
  return (
    <div className="bg-slate-600/50 rounded-lg p-3">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs text-slate-400 truncate max-w-[120px]" title={disk.mountpoint}>
          {disk.mountpoint}
        </span>
        <span className={`text-sm font-semibold ${getUsageColor(disk.usage)}`}>
          {disk.usage.toFixed(1)}%
        </span>
      </div>
      <div className="w-full bg-slate-700 rounded-full h-2 mb-2">
        <div
          className={`h-2 rounded-full transition-all ${getUsageBarColor(disk.usage)}`}
          style={{ width: `${Math.min(disk.usage, 100)}%` }}
        />
      </div>
      <div className="flex justify-between text-xs text-slate-500">
        <span>{formatBytes(disk.usedBytes)}</span>
        <span>{formatBytes(disk.totalBytes)}</span>
      </div>
      <p className="text-xs text-slate-500 truncate mt-1" title={disk.device}>
        {disk.device}
      </p>
    </div>
  );
}

function NetworkCard({ iface }: { iface: NetworkInterface }) {
  const isLoopback = iface.name === 'lo' || iface.name.startsWith('lo');

  if (isLoopback) return null;

  return (
    <div className="bg-slate-600/50 rounded-lg p-3">
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm font-medium text-slate-300">{iface.name}</span>
        {iface.ips && iface.ips.length > 0 && (
          <span className="text-xs text-slate-500 truncate max-w-[100px]" title={iface.ips[0]}>
            {iface.ips[0].split('/')[0]}
          </span>
        )}
      </div>
      <div className="grid grid-cols-2 gap-2">
        <div>
          <p className="text-xs text-slate-400 mb-1">Download</p>
          <p className="text-sm font-semibold text-green-400">
            ↓ {formatBytesPerSec(iface.recvRate)}
          </p>
          <p className="text-xs text-slate-500">{formatBytes(iface.recvBytes)}</p>
        </div>
        <div>
          <p className="text-xs text-slate-400 mb-1">Upload</p>
          <p className="text-sm font-semibold text-red-400">
            ↑ {formatBytesPerSec(iface.sentRate)}
          </p>
          <p className="text-xs text-slate-500">{formatBytes(iface.sentBytes)}</p>
        </div>
      </div>
    </div>
  );
}

export default function AgentCard({ agent, metrics, history, onDelete }: AgentCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [activeTab, setActiveTab] = useState<'overview' | 'disks' | 'network'>('overview');
  const isOnline = metrics?.online ?? false;

  const totalNetworkRx = metrics?.interfaces?.reduce((sum, i) => sum + (i.recvRate || 0), 0) ?? 0;
  const totalNetworkTx = metrics?.interfaces?.reduce((sum, i) => sum + (i.sentRate || 0), 0) ?? 0;

  const filteredInterfaces = metrics?.interfaces?.filter(
    (i) => i.name !== 'lo' && !i.name.startsWith('lo')
  ) ?? [];

  const totalDiskUsed = metrics?.disks?.reduce((sum, d) => sum + d.usedBytes, 0) ?? 0;
  const totalDiskTotal = metrics?.disks?.reduce((sum, d) => sum + d.totalBytes, 0) ?? 0;
  const avgDiskUsage = totalDiskTotal > 0 ? (totalDiskUsed / totalDiskTotal) * 100 : 0;

  return (
    <div className="bg-slate-800 rounded-lg p-6">
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold text-white">{agent.name}</h3>
            <span
              className={`inline-flex items-center gap-1.5 px-2 py-1 text-xs font-medium rounded-full ${isOnline ? 'bg-green-500/10 text-green-400' : 'bg-slate-500/10 text-slate-400'
                }`}
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${isOnline ? 'bg-green-400 animate-pulse' : 'bg-slate-400'
                  }`}
              />
              {isOnline ? 'Online' : 'Offline'}
            </span>
            {metrics?.os && (
              <span className="text-xs text-slate-500">
                {metrics.os} {metrics.platform && `(${metrics.platform})`}
              </span>
            )}
          </div>
          <p className="text-sm text-slate-400 font-mono mt-1">
            {agent.hostname || agent.nodeId || 'Unknown'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {isOnline && (
            <button
              onClick={() => setIsExpanded(!isExpanded)}
              className="px-3 py-1.5 text-sm text-slate-300 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
            >
              {isExpanded ? 'Collapse' : 'Expand'}
            </button>
          )}
          <button
            onClick={() => onDelete(agent.id)}
            className="text-red-400 hover:text-red-300 text-sm transition-colors"
          >
            Delete
          </button>
        </div>
      </div>

      {isOnline && metrics ? (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 mb-4">
            <div className="bg-slate-700/50 rounded-lg p-3">
              <p className="text-xs text-slate-400 mb-1">CPU</p>
              <p className={`text-lg font-semibold ${getUsageColor(metrics.cpuUsage ?? 0)}`}>
                {metrics.cpuUsage?.toFixed(1) ?? '-'}%
              </p>
            </div>
            <div className="bg-slate-700/50 rounded-lg p-3">
              <p className="text-xs text-slate-400 mb-1">Memory</p>
              <p className={`text-lg font-semibold ${getUsageColor(metrics.memoryUsage ?? 0)}`}>
                {metrics.memoryUsage?.toFixed(1) ?? '-'}%
              </p>
              <p className="text-xs text-slate-500">
                {formatBytes(metrics.memoryUsed)} / {formatBytes(metrics.memoryTotal)}
              </p>
            </div>
            <div className="bg-slate-700/50 rounded-lg p-3">
              <p className="text-xs text-slate-400 mb-1">
                Disk {metrics.disks && metrics.disks.length > 1 && `(${metrics.disks.length})`}
              </p>
              <p className={`text-lg font-semibold ${getUsageColor(avgDiskUsage)}`}>
                {avgDiskUsage.toFixed(1)}%
              </p>
              <p className="text-xs text-slate-500">
                {formatBytes(totalDiskUsed)} / {formatBytes(totalDiskTotal)}
              </p>
            </div>
            <div className="bg-slate-700/50 rounded-lg p-3">
              <p className="text-xs text-slate-400 mb-1">
                Network {filteredInterfaces.length > 1 && `(${filteredInterfaces.length})`}
              </p>
              <p className="text-sm font-semibold text-green-400">
                ↓ {formatBytesPerSec(totalNetworkRx)}
              </p>
              <p className="text-sm font-semibold text-red-400">
                ↑ {formatBytesPerSec(totalNetworkTx)}
              </p>
            </div>
            {metrics.temperature !== null && metrics.temperature !== undefined && metrics.temperature > 0 && (
              <div className="bg-slate-700/50 rounded-lg p-3">
                <p className="text-xs text-slate-400 mb-1">Temperature</p>
                <p
                  className={`text-lg font-semibold ${metrics.temperature > 80
                      ? 'text-red-400'
                      : metrics.temperature > 60
                        ? 'text-yellow-400'
                        : 'text-white'
                    }`}
                >
                  {metrics.temperature}°C
                </p>
              </div>
            )}
            <div className="bg-slate-700/50 rounded-lg p-3">
              <p className="text-xs text-slate-400 mb-1">Uptime</p>
              <p className="text-lg font-semibold text-white">
                {formatUptime(metrics.uptimeSeconds)}
              </p>
              {metrics.processCount !== null && (
                <p className="text-xs text-slate-500">{metrics.processCount} processes</p>
              )}
            </div>
          </div>

          {isExpanded && (
            <div className="mt-4 pt-4 border-t border-slate-700">
              <div className="flex gap-2 mb-4">
                <button
                  onClick={() => setActiveTab('overview')}
                  className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${activeTab === 'overview'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                    }`}
                >
                  Overview
                </button>
                <button
                  onClick={() => setActiveTab('disks')}
                  className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${activeTab === 'disks'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                    }`}
                >
                  Disks ({metrics.disks?.length ?? 0})
                </button>
                <button
                  onClick={() => setActiveTab('network')}
                  className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${activeTab === 'network'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                    }`}
                >
                  Network ({filteredInterfaces.length})
                </button>
              </div>

              {activeTab === 'overview' && (
                <>
                  {history.length > 1 ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <MetricsChart
                        data={history}
                        dataKey="cpuUsage"
                        title="CPU Usage"
                        color="#3b82f6"
                        unit="%"
                        maxY={100}
                      />
                      <MetricsChart
                        data={history}
                        dataKey="memoryUsage"
                        title="Memory Usage"
                        color="#8b5cf6"
                        unit="%"
                        maxY={100}
                      />
                      <MetricsChart
                        data={history}
                        dataKey="diskUsage"
                        title="Disk Usage"
                        color="#f59e0b"
                        unit="%"
                        maxY={100}
                      />
                      <MultiLineChart
                        data={history}
                        lines={[
                          { dataKey: 'networkRxRate', color: '#10b981', name: 'Download' },
                          { dataKey: 'networkTxRate', color: '#ef4444', name: 'Upload' },
                        ]}
                        title="Network I/O"
                        unit="bytes"
                      />
                      {history.some((h) => h.temperature !== null && h.temperature > 0) && (
                        <MetricsChart
                          data={history}
                          dataKey="temperature"
                          title="Temperature"
                          color="#ef4444"
                          unit="°C"
                        />
                      )}
                    </div>
                  ) : (
                    <div className="text-center text-slate-500 text-sm py-8">
                      Collecting data for graphs... Please wait.
                    </div>
                  )}
                </>
              )}

              {activeTab === 'disks' && (
                <div>
                  {metrics.disks && metrics.disks.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
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
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
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
          )}
        </>
      ) : (
        <div className="text-sm text-slate-500">
          No metrics available. Waiting for agent to connect...
        </div>
      )}
    </div>
  );
}
