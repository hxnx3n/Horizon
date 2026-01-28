import { useState, useEffect, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useMetricsStream } from '../hooks/useMetricsStream';
import { useMetricsHistory } from '../hooks/useMetricsHistory';
import { getAgents, deleteAgent } from '../api/agent';
import { getClientKeys, createClientKey, revokeClientKey, deleteClientKey, type ClientKey, type ClientKeyCreatedResponse } from '../api/clientKey';
import AgentCard from '../components/AgentCard';
import type { Agent, RealtimeMetrics } from '../types/agent';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const [agents, setAgents] = useState<Agent[]>([]);
  const [clientKeys, setClientKeys] = useState<ClientKey[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Key modal state
  const [isKeyModalOpen, setIsKeyModalOpen] = useState(false);
  const [newKeyExpiresInDays, setNewKeyExpiresInDays] = useState<number | undefined>(undefined);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // Created key display
  const [createdKey, setCreatedKey] = useState<ClientKeyCreatedResponse | null>(null);
  const [copied, setCopied] = useState(false);

  // Tab state
  const [activeTab, setActiveTab] = useState<'agents' | 'keys'>('agents');

  const { addMetrics, addMultipleMetrics, getHistory } = useMetricsHistory({ maxPoints: 60 });

  const handleMetrics = (metricsData: RealtimeMetrics | RealtimeMetrics[]) => {
    if (Array.isArray(metricsData)) {
      addMultipleMetrics(metricsData);
    } else {
      addMetrics(metricsData);
    }
  };

  const { metrics, isConnected, error: streamError } = useMetricsStream({
    onMetrics: handleMetrics,
  });

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

  const fetchClientKeys = async () => {
    try {
      const response = await getClientKeys();
      if (response.success && response.data) {
        setClientKeys(response.data);
      }
    } catch {
      console.error('Failed to fetch client keys');
    }
  };

  useEffect(() => {
    fetchAgents();
    fetchClientKeys();
  }, []);

  const handleCreateKey = async (e: FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    try {
      const response = await createClientKey({
        expiresInDays: newKeyExpiresInDays,
      });
      if (response.success && response.data) {
        setCreatedKey(response.data);
        fetchClientKeys();
        setNewKeyExpiresInDays(undefined);
      } else {
        setError(response.message || 'Failed to create key');
      }
    } catch {
      setError('An error occurred while creating key');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRevokeKey = async (id: number) => {
    if (!confirm('Are you sure you want to revoke this key? Agents using this key will no longer be able to push metrics.')) return;

    try {
      const response = await revokeClientKey(id);
      if (response.success) {
        fetchClientKeys();
      } else {
        setError(response.message || 'Failed to revoke key');
      }
    } catch {
      setError('An error occurred while revoking key');
    }
  };

  const handleDeleteKey = async (id: number) => {
    if (!confirm('Are you sure you want to delete this key? This action cannot be undone.')) return;

    try {
      const response = await deleteClientKey(id);
      if (response.success) {
        fetchClientKeys();
      } else {
        setError(response.message || 'Failed to delete key');
      }
    } catch {
      setError('An error occurred while deleting key');
    }
  };

  const handleDeleteAgent = async (id: number) => {
    if (!confirm('Are you sure you want to delete this agent?')) return;

    try {
      const response = await deleteAgent(id);
      if (response.success) {
        setAgents(agents.filter((agent) => agent.id !== id));
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

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      console.error('Failed to copy');
    }
  };

  const closeKeyModal = () => {
    setIsKeyModalOpen(false);
    setCreatedKey(null);
    setNewKeyExpiresInDays(undefined);
    setCopied(false);
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="min-h-screen bg-slate-900">
      <header className="bg-slate-800 border-b border-slate-700">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-white">Horizon</h1>
            <span
              className={`inline-flex items-center gap-1.5 px-2 py-1 text-xs font-medium rounded-full ${isConnected ? 'bg-green-500/10 text-green-400' : 'bg-yellow-500/10 text-yellow-400'
                }`}
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${isConnected ? 'bg-green-400' : 'bg-yellow-400'
                  }`}
              />
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
        {/* Tabs */}
        <div className="flex items-center gap-4 mb-6 border-b border-slate-700">
          <button
            onClick={() => setActiveTab('agents')}
            className={`pb-3 px-1 text-sm font-medium border-b-2 transition-colors ${
              activeTab === 'agents'
                ? 'border-blue-500 text-blue-400'
                : 'border-transparent text-slate-400 hover:text-slate-300'
            }`}
          >
            Agents ({agents.length})
          </button>
          <button
            onClick={() => setActiveTab('keys')}
            className={`pb-3 px-1 text-sm font-medium border-b-2 transition-colors ${
              activeTab === 'keys'
                ? 'border-blue-500 text-blue-400'
                : 'border-transparent text-slate-400 hover:text-slate-300'
            }`}
          >
            Client Keys ({clientKeys.length})
          </button>
        </div>

        {(error || streamError) && (
          <div className="mb-4 bg-red-500/10 border border-red-500/50 rounded-lg p-3 text-red-400 text-sm">
            {error || streamError}
          </div>
        )}

        {activeTab === 'agents' && (
          <>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-semibold text-white">Agents</h2>
              <button
                onClick={() => setIsKeyModalOpen(true)}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
              >
                Generate Client Key
              </button>
            </div>

            {isLoading ? (
              <div className="text-slate-400">Loading agents...</div>
            ) : agents.length === 0 ? (
              <div className="bg-slate-800 rounded-lg p-8 text-center">
                <p className="text-slate-400 mb-4">No agents registered yet.</p>
                <p className="text-slate-500 text-sm mb-4">
                  Generate a client key and use it to register your node:
                </p>
                <div className="bg-slate-900 rounded-lg p-4 text-left max-w-md mx-auto mb-4">
                  <code className="text-green-400 text-sm">
                    horizon-agent auth &lt;your-client-key&gt; &lt;server-url&gt;<br />
                    horizon-agent run
                  </code>
                </div>
                <button
                  onClick={() => setIsKeyModalOpen(true)}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
                >
                  Generate Client Key
                </button>
              </div>
            ) : (
              <div className="grid gap-4">
                {agents.map((agent) => (
                  <AgentCard
                    key={agent.id}
                    agent={agent}
                    metrics={metrics.get(agent.id)}
                    history={getHistory(agent.id)}
                    onDelete={handleDeleteAgent}
                  />
                ))}
              </div>
            )}
          </>
        )}

        {activeTab === 'keys' && (
          <>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-semibold text-white">Client Keys</h2>
              <button
                onClick={() => setIsKeyModalOpen(true)}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
              >
                Generate New Key
              </button>
            </div>

            {clientKeys.length === 0 ? (
              <div className="bg-slate-800 rounded-lg p-8 text-center">
                <p className="text-slate-400 mb-4">No client keys created yet.</p>
                <p className="text-slate-500 text-sm mb-4">
                  Create a client key to authenticate your node.
                </p>
                <button
                  onClick={() => setIsKeyModalOpen(true)}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
                >
                  Create Your First Key
                </button>
              </div>
            ) : (
              <div className="bg-slate-800 rounded-lg overflow-hidden">
                <table className="w-full">
                  <thead className="bg-slate-700">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">Name</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">Key</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">Status</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">Last Used</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">Created</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-slate-300 uppercase tracking-wider">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-700">
                    {clientKeys.map((key) => (
                      <tr key={key.id} className="hover:bg-slate-700/50">
                        <td className="px-4 py-3">
                          <div className="text-white font-medium">{key.name}</div>
                          {key.description && (
                            <div className="text-slate-400 text-sm">{key.description}</div>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <code className="text-slate-300 bg-slate-900 px-2 py-1 rounded text-sm">
                            {key.maskedKey}
                          </code>
                        </td>
                        <td className="px-4 py-3">
                          {key.expired ? (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-500/10 text-red-400">
                              Expired
                            </span>
                          ) : key.enabled ? (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-500/10 text-green-400">
                              Active
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-500/10 text-yellow-400">
                              Revoked
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-slate-400 text-sm">
                          {formatDate(key.lastUsedAt)}
                        </td>
                        <td className="px-4 py-3 text-slate-400 text-sm">
                          {formatDate(key.createdAt)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {key.enabled && !key.expired && (
                              <button
                                onClick={() => handleRevokeKey(key.id)}
                                className="px-3 py-1 text-sm text-yellow-400 hover:text-yellow-300 transition-colors"
                              >
                                Revoke
                              </button>
                            )}
                            <button
                              onClick={() => handleDeleteKey(key.id)}
                              className="px-3 py-1 text-sm text-red-400 hover:text-red-300 transition-colors"
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Usage instructions */}
            <div className="mt-8 bg-slate-800 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-white mb-4">How to Use</h3>
              <div className="space-y-4 text-slate-300">
                <div>
                  <p className="font-medium text-white mb-2">1. Generate a Client Key</p>
                  <p className="text-sm text-slate-400">Click "Generate New Key" and save the key securely. Each key is for one node.</p>
                </div>
                <div>
                  <p className="font-medium text-white mb-2">2. Authenticate Your Agent</p>
                  <div className="bg-slate-900 rounded-lg p-3 mt-2">
                    <code className="text-green-400 text-sm">
                      horizon-agent auth &lt;your-client-key&gt; http://your-server:8080
                    </code>
                  </div>
                </div>
                <div>
                  <p className="font-medium text-white mb-2">3. Start the Agent</p>
                  <div className="bg-slate-900 rounded-lg p-3 mt-2">
                    <code className="text-green-400 text-sm">
                      horizon-agent run
                    </code>
                  </div>
                </div>
                <p className="text-sm text-slate-400">
                  The agent will automatically register and appear in your dashboard when it starts pushing metrics.
                </p>
              </div>
            </div>
          </>
        )}
      </main>

      {/* Create Key Modal */}
      {isKeyModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-800 rounded-2xl shadow-xl p-6 w-full max-w-md">
            {createdKey ? (
              <>
                <h3 className="text-lg font-semibold text-white mb-4">Client Key Created!</h3>
                <div className="bg-yellow-500/10 border border-yellow-500/50 rounded-lg p-3 mb-4">
                  <p className="text-yellow-400 text-sm font-medium">
                    ⚠️ Save this key now! You won't be able to see it again.
                  </p>
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-slate-300 mb-2">Your Client Key</label>
                  <div className="flex items-center gap-2">
                    <code className="flex-1 bg-slate-900 px-3 py-2 rounded-lg text-green-400 text-sm break-all">
                      {createdKey.keyValue}
                    </code>
                    <button
                      onClick={() => copyToClipboard(createdKey.keyValue)}
                      className="px-3 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition-colors"
                    >
                      {copied ? '✓' : 'Copy'}
                    </button>
                  </div>
                </div>
                <div className="bg-slate-900 rounded-lg p-4 mb-4">
                  <p className="text-slate-400 text-sm mb-2">Get Started:</p>
                  <a
                    href="https://github.com/hxnx3n/Horizon/releases/latest"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-2 text-blue-400 hover:text-blue-300 text-sm transition-colors"
                  >
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                    </svg>
                    Download Horizon Agent
                  </a>
                </div>
                <button
                  onClick={closeKeyModal}
                  className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
                >
                  Done
                </button>
              </>
            ) : (
              <>
                <h3 className="text-lg font-semibold text-white mb-4">Generate Client Key</h3>
                <form onSubmit={handleCreateKey} className="space-y-4">
                  <div>
                    <label
                      htmlFor="keyExpiry"
                      className="block text-sm font-medium text-slate-300 mb-2"
                    >
                      Expires In (days)
                    </label>
                    <select
                      id="keyExpiry"
                      value={newKeyExpiresInDays || ''}
                      onChange={(e) => setNewKeyExpiresInDays(e.target.value ? Number(e.target.value) : undefined)}
                      className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    >
                      <option value="">Never expires</option>
                      <option value="7">7 days</option>
                      <option value="30">30 days</option>
                      <option value="90">90 days</option>
                      <option value="365">1 year</option>
                    </select>
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button
                      type="button"
                      onClick={closeKeyModal}
                      className="flex-1 py-3 px-4 bg-slate-700 hover:bg-slate-600 text-white font-medium rounded-lg transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isSubmitting}
                      className="flex-1 py-3 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors"
                    >
                      {isSubmitting ? 'Creating...' : 'Create Key'}
                    </button>
                  </div>
                </form>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
