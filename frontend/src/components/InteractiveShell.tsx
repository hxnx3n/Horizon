import { useEffect, useRef, useCallback } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { WebLinksAddon } from '@xterm/addon-web-links';
import '@xterm/xterm/css/xterm.css';
import '../styles/InteractiveShell.css';

interface InteractiveShellProps {
  agentId: string;
  hostname?: string;
  onClose?: () => void;
}

export function InteractiveShell({ agentId, hostname, onClose }: InteractiveShellProps) {
  const terminalRef = useRef<HTMLDivElement>(null);
  const xtermRef = useRef<Terminal | null>(null);
  const fitAddonRef = useRef<FitAddon | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const isConnectedRef = useRef(false);

  const sendMessage = useCallback((type: string, data: string = '', cols: number = 0, rows: number = 0) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type, data, cols, rows }));
    }
  }, []);

  useEffect(() => {
    if (!terminalRef.current) return;

    const term = new Terminal({
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
        cursor: '#ffffff',
        cursorAccent: '#1e1e1e',
        selectionBackground: '#264f78',
        black: '#000000',
        red: '#cd3131',
        green: '#0dbc79',
        yellow: '#e5e510',
        blue: '#2472c8',
        magenta: '#bc3fbc',
        cyan: '#11a8cd',
        white: '#e5e5e5',
        brightBlack: '#666666',
        brightRed: '#f14c4c',
        brightGreen: '#23d18b',
        brightYellow: '#f5f543',
        brightBlue: '#3b8eea',
        brightMagenta: '#d670d6',
        brightCyan: '#29b8db',
        brightWhite: '#e5e5e5',
      },
      fontFamily: '"JetBrains Mono", "Fira Code", "Monaco", "Menlo", "Ubuntu Mono", monospace',
      fontSize: 14,
      lineHeight: 1.2,
      cursorBlink: true,
      cursorStyle: 'block',
      scrollback: 5000,
      convertEol: true,
    });

    const fitAddon = new FitAddon();
    const webLinksAddon = new WebLinksAddon();

    term.loadAddon(fitAddon);
    term.loadAddon(webLinksAddon);
    term.open(terminalRef.current);

    setTimeout(() => fitAddon.fit(), 0);

    xtermRef.current = term;
    fitAddonRef.current = fitAddon;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/api/ws/shell?agentId=${agentId}`;

    term.writeln('\x1b[1;33mConnecting to agent...\x1b[0m');

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      isConnectedRef.current = true;
      term.writeln('\x1b[1;32mConnected! Starting shell...\x1b[0m\r\n');

      const { cols, rows } = term;
      ws.send(JSON.stringify({ type: 'shell_start', data: '', cols, rows }));
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);

        if (msg.type === 'shell_output') {
          term.write(msg.data);
        } else if (msg.type === 'shell_exit') {
          term.writeln('\r\n\x1b[1;33mShell session ended.\x1b[0m');
          isConnectedRef.current = false;
        } else if (msg.type === 'error') {
          term.writeln(`\r\n\x1b[1;31mError: ${msg.data}\x1b[0m`);
        }
      } catch (e) {
        console.error('Failed to parse WebSocket message:', e);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      term.writeln('\r\n\x1b[1;31mConnection error\x1b[0m');
    };

    ws.onclose = () => {
      isConnectedRef.current = false;
      term.writeln('\r\n\x1b[1;33mDisconnected from agent.\x1b[0m');
    };

    term.onData((data) => {
      if (isConnectedRef.current) {
        sendMessage('shell_input', data);
      }
    });

    const handleResize = () => {
      fitAddon.fit();
      if (isConnectedRef.current) {
        const { cols, rows } = term;
        sendMessage('shell_resize', '', cols, rows);
      }
    };

    term.onResize(({ cols, rows }) => {
      if (isConnectedRef.current) {
        sendMessage('shell_resize', '', cols, rows);
      }
    });

    window.addEventListener('resize', handleResize);
    term.focus();

    return () => {
      window.removeEventListener('resize', handleResize);
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'shell_stop' }));
        ws.close();
      }
      term.dispose();
    };
  }, [agentId, sendMessage]);

  useEffect(() => {
    if (!terminalRef.current || !fitAddonRef.current) return;

    const resizeObserver = new ResizeObserver(() => {
      fitAddonRef.current?.fit();
    });

    resizeObserver.observe(terminalRef.current);

    return () => {
      resizeObserver.disconnect();
    };
  }, []);

  return (
    <div className="interactive-shell">
      <div className="shell-header">
        <span className="shell-title">
          Interactive Shell {hostname ? `- ${hostname}` : ''} (Agent {agentId})
        </span>
        {onClose && (
          <button className="shell-close-btn" onClick={onClose} title="Close shell">
            ✕
          </button>
        )}
      </div>
      <div ref={terminalRef} className="shell-terminal" />
    </div>
  );
}
