import { useState, useRef, useEffect } from 'react';
import '../styles/ConsoleTerminal.css';

interface ConsoleLog {
  id: number;
  type: 'input' | 'output' | 'error';
  content: string;
  timestamp: Date;
}

interface ConsoleTerminalProps {
  agentId?: string;
  onCommand?: (command: string) => Promise<string>;
}

export function ConsoleTerminal({ agentId, onCommand }: ConsoleTerminalProps) {
  const [logs, setLogs] = useState<ConsoleLog[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const logsEndRef = useRef<HTMLDivElement>(null);
  const logCountRef = useRef(0);

  const scrollToBottom = () => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [logs]);

  const addLog = (type: ConsoleLog['type'], content: string) => {
    logCountRef.current += 1;
    setLogs((prev) => [
      ...prev,
      {
        id: logCountRef.current,
        type,
        content,
        timestamp: new Date(),
      },
    ]);
  };

  const handleExecuteCommand = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!input.trim()) return;

    addLog('input', input);
    setIsLoading(true);

    try {
      if (onCommand) {
        const result = await onCommand(input);
        addLog('output', result);
      } else {
        // 기본 동작: 로컬 명령어 시뮬레이션
        addLog('output', `Command executed: ${input}`);
      }
    } catch (error) {
      addLog('error', error instanceof Error ? error.message : String(error));
    } finally {
      setInput('');
      setIsLoading(false);
    }
  };

  const handleClear = () => {
    setLogs([]);
  };

  return (
    <div className="console-terminal">
      <div className="console-header">
        <h3>
          Terminal
          {agentId && <span className="agent-id">Agent: {agentId}</span>}
        </h3>
        <button className="clear-btn" onClick={handleClear} title="Clear console">
          Clear
        </button>
      </div>

      <div className="console-logs">
        {logs.length === 0 ? (
          <div className="console-empty">Ready to execute commands...</div>
        ) : (
          logs.map((log) => (
            <div key={log.id} className={`log-entry log-${log.type}`}>
              <span className="log-time">
                {log.timestamp.toLocaleTimeString()}
              </span>
              <span className="log-prefix">
                {log.type === 'input' ? '$ ' : ''}
              </span>
              <span className="log-content">{log.content}</span>
            </div>
          ))
        )}
        <div ref={logsEndRef} />
      </div>

      <form onSubmit={handleExecuteCommand} className="console-input-form">
        <div className="input-wrapper">
          <span className="prompt">$</span>
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Enter command..."
            disabled={isLoading}
            className="console-input"
            autoFocus
          />
        </div>
        <button
          type="submit"
          disabled={isLoading || !input.trim()}
          className="execute-btn"
        >
          {isLoading ? 'Executing...' : 'Execute'}
        </button>
      </form>
    </div>
  );
}
