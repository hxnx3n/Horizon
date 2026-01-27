import { useMemo, memo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts';
import type { MetricsHistoryPoint } from '../types/agent';

interface MetricsChartProps {
  data: MetricsHistoryPoint[];
  dataKey: keyof MetricsHistoryPoint;
  title: string;
  color: string;
  unit?: string;
  type?: 'line' | 'area';
  minY?: number;
  maxY?: number;
  formatValue?: (value: number) => string;
  height?: number;
}

function formatTime(timestamp: number): string {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('en-US', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B/s';
  const k = 1024;
  const sizes = ['B/s', 'KB/s', 'MB/s', 'GB/s'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

const MetricsChart = memo(function MetricsChart({
  data,
  dataKey,
  title,
  color,
  unit = '%',
  type = 'area',
  minY = 0,
  maxY,
  formatValue,
  height = 200,
}: MetricsChartProps) {
  const chartData = useMemo(() => {
    return data.map((point) => ({
      ...point,
      time: formatTime(point.timestamp),
      [dataKey]: point[dataKey] ?? null,
    }));
  }, [data, dataKey]);

  const domain: [number, number | 'auto'] = useMemo(() => {
    if (maxY !== undefined) {
      return [minY, maxY];
    }
    return [minY, 'auto'];
  }, [minY, maxY]);

  const tooltipFormatter = (value: number | undefined) => {
    const v = value ?? 0;
    if (formatValue) {
      return formatValue(v);
    }
    if (unit === 'bytes') {
      return formatBytes(v);
    }
    return `${v.toFixed(1)}${unit}`;
  };

  const yAxisFormatter = (value: number) => {
    if (unit === 'bytes') {
      return formatBytes(value);
    }
    return `${value}${unit}`;
  };

  const gradientId = `gradient-${dataKey}-${title.replace(/\s/g, '')}`;

  return (
    <div className="bg-slate-700/50 rounded-lg p-4">
      <h4 className="text-sm font-medium text-slate-300 mb-3">{title}</h4>
      <ResponsiveContainer width="100%" height={height}>
        {type === 'area' ? (
          <AreaChart data={chartData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={color} stopOpacity={0.3} />
                <stop offset="95%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
            <XAxis
              dataKey="time"
              stroke="#6b7280"
              fontSize={10}
              tickLine={false}
              interval="preserveStartEnd"
            />
            <YAxis
              stroke="#6b7280"
              fontSize={10}
              tickLine={false}
              domain={domain}
              tickFormatter={yAxisFormatter}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#1e293b',
                border: '1px solid #374151',
                borderRadius: '8px',
                fontSize: '12px',
              }}
              labelStyle={{ color: '#94a3b8' }}
              itemStyle={{ color: color }}
              formatter={(value: number | undefined) => [tooltipFormatter(value), title]}
            />
            <Area
              type="monotone"
              dataKey={dataKey}
              stroke={color}
              strokeWidth={2}
              fill={`url(#${gradientId})`}
              isAnimationActive={false}
              connectNulls={true}
            />
          </AreaChart>
        ) : (
          <LineChart data={chartData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
            <XAxis
              dataKey="time"
              stroke="#6b7280"
              fontSize={10}
              tickLine={false}
              interval="preserveStartEnd"
            />
            <YAxis
              stroke="#6b7280"
              fontSize={10}
              tickLine={false}
              domain={domain}
              tickFormatter={yAxisFormatter}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#1e293b',
                border: '1px solid #374151',
                borderRadius: '8px',
                fontSize: '12px',
              }}
              labelStyle={{ color: '#94a3b8' }}
              itemStyle={{ color: color }}
              formatter={(value: number | undefined) => [tooltipFormatter(value), title]}
            />
            <Line
              type="monotone"
              dataKey={dataKey}
              stroke={color}
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
              connectNulls={true}
            />
          </LineChart>
        )}
      </ResponsiveContainer>
    </div>
  );
});

interface MultiLineChartProps {
  data: MetricsHistoryPoint[];
  lines: {
    dataKey: keyof MetricsHistoryPoint;
    color: string;
    name: string;
  }[];
  title: string;
  unit?: string;
  height?: number;
  formatValue?: (value: number) => string;
}

const MultiLineChart = memo(function MultiLineChart({
  data,
  lines,
  title,
  unit = '%',
  height = 200,
  formatValue,
}: MultiLineChartProps) {
  const chartData = useMemo(() => {
    return data.map((point) => ({
      ...point,
      time: formatTime(point.timestamp),
    }));
  }, [data]);

  const tooltipFormatter = (value: number | undefined) => {
    const v = value ?? 0;
    if (formatValue) {
      return formatValue(v);
    }
    if (unit === 'bytes') {
      return formatBytes(v);
    }
    return `${v.toFixed(1)}${unit}`;
  };

  const yAxisFormatter = (value: number) => {
    if (unit === 'bytes') {
      return formatBytes(value);
    }
    return `${value}${unit}`;
  };

  return (
    <div className="bg-slate-700/50 rounded-lg p-4">
      <h4 className="text-sm font-medium text-slate-300 mb-3">{title}</h4>
      <ResponsiveContainer width="100%" height={height}>
        <LineChart data={chartData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
          <XAxis
            dataKey="time"
            stroke="#6b7280"
            fontSize={10}
            tickLine={false}
            interval="preserveStartEnd"
          />
          <YAxis
            stroke="#6b7280"
            fontSize={10}
            tickLine={false}
            tickFormatter={yAxisFormatter}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#1e293b',
              border: '1px solid #374151',
              borderRadius: '8px',
              fontSize: '12px',
            }}
            labelStyle={{ color: '#94a3b8' }}
            formatter={(value: number | undefined, name: string | undefined) => [tooltipFormatter(value), name ?? '']}
          />
          {lines.map((line) => (
            <Line
              key={line.dataKey}
              type="monotone"
              dataKey={line.dataKey}
              stroke={line.color}
              strokeWidth={2}
              dot={false}
              name={line.name}
              isAnimationActive={false}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
});

export default MetricsChart;
export { MultiLineChart };
