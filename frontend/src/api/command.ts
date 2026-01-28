import axios from 'axios';

interface CommandExecutionResponse {
  output: string;
  error: string;
  exitCode: number;
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export async function executeAgentCommand(
  agentId: string,
  command: string
): Promise<string> {
  try {
    const response = await axios.post<CommandExecutionResponse>(
      `${API_BASE_URL}/api/agents/${agentId}/command`,
      { command },
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    const { output, error, exitCode } = response.data;
    
    let result = '';
    if (output) result += output;
    if (error) result += (result ? '\n' : '') + error;
    
    if (exitCode !== 0 && !result) {
      result = `Command failed with exit code: ${exitCode}`;
    }

    return result || 'Command executed successfully';
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string })?.error ||
        error.message ||
        'Failed to execute command';
      throw new Error(message);
    }
    throw error;
  }
}
