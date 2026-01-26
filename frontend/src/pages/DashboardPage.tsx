import { useState, useEffect, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';
import { getAgents, createAgent, deleteAgent } from '../api/agent';
import type { Agent } from '../types/agent';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const [agents, setAgents] = useState<Agent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newAgentName, setNewAgentName] = useState('');
  const [newAgentIp, setNewAgentIp] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

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
      const response = await createAgent({ name: newAgentName, ip: newAgentIp });
      if (response.success && response.data) {
        setAgents([...agents, response.data]);
        setIsModalOpen(false);
        setNewAgentName('');
        setNewAgentIp('');
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

  return (
    <div className="min-h-screen bg-slate-900">
      <header className="bg-slate-800 border-b border-slate-700">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-white">Horizon</h1>
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

        {error && (
          <div className="mb-4 bg-red-500/10 border border-red-500/50 rounded-lg p-3 text-red-400 text-sm">
            {error}
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
          <div className="bg-slate-800 rounded-lg overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-700">
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                    IP Address
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Created
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700">
                {agents.map((agent) => (
                  <tr key={agent.id} className="hover:bg-slate-700/50">
                    <td className="px-6 py-4 text-sm text-white">{agent.name}</td>
                    <td className="px-6 py-4 text-sm text-slate-300 font-mono">{agent.ip}</td>
                    <td className="px-6 py-4">
                      <span
                        className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${agent.enabled
                          ? 'bg-green-500/10 text-green-400'
                          : 'bg-red-500/10 text-red-400'
                          }`}
                      >
                        {agent.enabled ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-400">
                      {new Date(agent.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleDeleteAgent(agent.id)}
                        className="text-red-400 hover:text-red-300 text-sm transition-colors"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
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
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setIsModalOpen(false);
                    setNewAgentName('');
                    setNewAgentIp('');
                  }}
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
