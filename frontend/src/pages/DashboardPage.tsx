import { useState, useEffect, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useMetricsStream } from '../hooks/useMetricsStream';
import { getAgents, createAgent, deleteAgent } from '../api/agent';
import type { Agent, RealtimeMetrics } from '../types/agent';

function formatBytes(bytes: number | null): string {
  if (bytes === null || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const [agents, setAgents] = useState<Agent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newAgentName, setNewAgentName] = useState('');
  const [newAgentIp, setNewAgentIp] = useState('');
  const [newAgentPort, setNewAgentPort] = useState(9090);
  const [newAgentPollingInterval, setNewAgentPollingInterval] = useState(1000);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { metrics, isConnected, error: streamError } = useMetricsStream();

  const fetchAgents = async () => {
    try {
      const response = await getAgents();
      if (response.success && response.data) {
        setAgents(response.data);
      } else {
        setError(response.message || 'Failed to fetch agents');
      }
    } catch {
      setError('An error occurred while fetching agents');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAgents();
  }, []);

  const handleAddAgent = async (e: FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    try {
      const response = await createAgent({
        name: newAgentName,
        ip: newAgentIp,
        port: newAgentPort,
        pollingInterval: newAgentPollingInterval,
      });
      if (response.success && response.data) {
        setAgents([...agents, response.data]);
        setIsModalOpen(false);
        setNewAgentName('');
        setNewAgentIp('');
        setNewAgentPort(9090);
        setNewAgentPollingInterval(1000);
      } else {
        setError(response.message || 'Failed to create agent');
      }
    } catch {
      setError('An error occurred while creating agent');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteAgent = async (id: number) => {
    if (!confirm('Are you sure you want to delete this agent?')) return;

    try {
      const response = await deleteAgent(id);
      if (response.success) {
        setAgents(agents.filter(agent => agent.id !== id));
      } else {
        setError(response.message || 'Failed to delete agent');
      }
    } catch {
      setError('An error occurred while deleting agent');
    }
  };

  const handleLogout = async () => {
    await logout();
  };

  const getAgentMetrics = (agentId: number): RealtimeMetrics | undefined => {
    return metrics.get(agentId);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setNewAgentName('');
    setNewAgentIp('');
    setNewAgentPort(9090);
    setNewAgentPollingInterval(1000);
  };

  return (
    <div className="min-h-screen bg-slate-900">
      <header className="bg-slate-800 border-b border-slate-700">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-white">Horizon</h1>
            <span className={`inline-flex items-center gap-1.5 px-2 py-1 text-xs font-medium rounded-full ${isConnected ? 'bg-green-500/10 text-green-400' : 'bg-yellow-500/10 text-yellow-400'
              }`}>
              <span className={`w-1.5 h-1.5 rounded-full ${isConnected ? 'bg-green-400' : 'bg-yellow-400'}`} />
              {isConnected ? 'Live' : 'Connecting...'}
            </span>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-slate-300">{user?.name}</span>
            <button
              onClick={handleLogout}
              className="px-4 py-2 text-sm text-slate-300 hover:text-white transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-white">Agents</h2>
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
          >
            Add Agent
          </button>
        </div>

        {(error || streamError) && (
          <div className="mb-4 bg-red-500/10 border border-red-500/50 rounded-lg p-3 text-red-400 text-sm">
            {error || streamError}
          </div>
        )}

        {isLoading ? (
          <div className="text-slate-400">Loading agents...</div>
        ) : agents.length === 0 ? (
          <div className="bg-slate-800 rounded-lg p-8 text-center">
            <p className="text-slate-400">No agents registered yet.</p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
            >
              Add your first agent
            </button>
          </div>
        ) : (
          <div className="grid gap-4">
            {agents.map((agent) => {
              const agentMetrics = getAgentMetrics(agent.id);
              const isOnline = agentMetrics?.online ?? false;

              return (
                <div key={agent.id} className="bg-slate-800 rounded-lg p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <div className="flex items-center gap-3">
                        <h3 className="text-lg font-semibold text-white">{agent.name}</h3>
                        <span className={`inline-flex items-center gap-1.5 px-2 py-1 text-xs font-medium rounded-full ${isOnline ? 'bg-green-500/10 text-green-400' : 'bg-slate-500/10 text-slate-400'
                          }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${isOnline ? 'bg-green-400 animate-pulse' : 'bg-slate-400'}`} />
                          {isOnline ? 'Online' : 'Offline'}
                        </span>
                      </div>
                      <p className="text-sm text-slate-400 font-mono mt-1">
                        {agent.ip}:{agent.port} · {agent.pollingInterval}ms
                      </p>
                    </div>
                    <button
                      onClick={() => handleDeleteAgent(agent.id)}
                      className="text-red-400 hover:text-red-300 text-sm transition-colors"
                    >
                      Delete
                    </button>
                  </div>

                  {isOnline && agentMetrics ? (
                    <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
                      <div className="bg-slate-700/50 rounded-lg p-3">
                        <p className="text-xs text-slate-400 mb-1">CPU</p>
                        <p className="text-lg font-semibold text-white">
                          {agentMetrics.cpuUsage?.toFixed(1) ?? '-'}%
                        </p>
                      </div>
                      <div className="bg-slate-700/50 rounded-lg p-3">
                        <p className="text-xs text-slate-400 mb-1">Memory</p>
                        <p className="text-lg font-semibold text-white">
                          {agentMetrics.memoryUsage?.toFixed(1) ?? '-'}%
                        </p>
                        <p className="text-xs text-slate-500">
                          {formatBytes(agentMetrics.memoryUsed)} / {formatBytes(agentMetrics.memoryTotal)}
                        </p>
                      </div>
                      <div className="bg-slate-700/50 rounded-lg p-3">
                        <p className="text-xs text-slate-400 mb-1">Disk</p>
                        <p className="text-lg font-semibold text-white">
                          {agentMetrics.diskUsage?.toFixed(1) ?? '-'}%
                        </p>
                        <p className="text-xs text-slate-500">
                          {formatBytes(agentMetrics.diskUsed)} / {formatBytes(agentMetrics.diskTotal)}
                        </p>
                      </div>
                      <div className="bg-slate-700/50 rounded-lg p-3">
                        <p className="text-xs text-slate-400 mb-1">Network RX/TX</p>
                        <p className="text-sm font-semibold text-white">
                          ↓{formatBytes(agentMetrics.networkRxBytes)}
                        </p>
                        <p className="text-sm font-semibold text-white">
                          ↑{formatBytes(agentMetrics.networkTxBytes)}
                        </p>
                      </div>
                      <div className="bg-slate-700/50 rounded-lg p-3">
                        <p className="text-xs text-slate-400 mb-1">Load Avg</p>
                        <p className="text-lg font-semibold text-white">
                          {agentMetrics.loadAverage1m?.toFixed(2) ?? '-'}
                        </p>
                        <p className="text-xs text-slate-500">
                          {agentMetrics.loadAverage5m?.toFixed(2)} / {agentMetrics.loadAverage15m?.toFixed(2)}
                        </p>
                      </div>
                      <div className="bg-slate-700/50 rounded-lg p-3">
                        <p className="text-xs text-slate-400 mb-1">Uptime</p>
                        <p className="text-lg font-semibold text-white">
                          {formatUptime(agentMetrics.uptimeSeconds)}
                        </p>
                        <p className="text-xs text-slate-500">
                          {agentMetrics.processCount ?? '-'} processes
                        </p>
                      </div>
                    </div>
                  ) : (
                    <div className="text-sm text-slate-500">
                      No metrics available. Waiting for agent to connect...
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </main>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-800 rounded-2xl shadow-xl p-6 w-full max-w-md">
            <h3 className="text-lg font-semibold text-white mb-4">Add New Agent</h3>
            <form onSubmit={handleAddAgent} className="space-y-4">
              <div>
                <label htmlFor="agentName" className="block text-sm font-medium text-slate-300 mb-2">
                  Name
                </label>
                <input
                  id="agentName"
                  type="text"
                  value={newAgentName}
                  onChange={(e) => setNewAgentName(e.target.value)}
                  required
                  className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  placeholder="Enter agent name"
                />
              </div>
              <div>
                <label htmlFor="agentIp" className="block text-sm font-medium text-slate-300 mb-2">
                  IP Address
                </label>
                <input
                  id="agentIp"
                  type="text"
                  value={newAgentIp}
                  onChange={(e) => setNewAgentIp(e.target.value)}
                  required
                  pattern="^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$"
                  className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  placeholder="e.g., 192.168.1.100"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="agentPort" className="block text-sm font-medium text-slate-300 mb-2">
                    Port
                  </label>
                  <input
                    id="agentPort"
                    type="number"
                    value={newAgentPort}
                    onChange={(e) => setNewAgentPort(Number(e.target.value))}
                    min={1}
                    max={65535}
                    required
                    className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  />
                </div>
                <div>
                  <label htmlFor="agentPollingInterval" className="block text-sm font-medium text-slate-300 mb-2">
                    Polling (ms)
                  </label>
                  <input
                    id="agentPollingInterval"
                    type="number"
                    value={newAgentPollingInterval}
                    onChange={(e) => setNewAgentPollingInterval(Number(e.target.value))}
                    min={100}
                    required
                    className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  />
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeModal}
                  className="flex-1 py-3 px-4 bg-slate-700 hover:bg-slate-600 text-white font-medium rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 py-3 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors"
                >
                  {isSubmitting ? 'Adding...' : 'Add Agent'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
