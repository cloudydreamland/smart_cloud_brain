/**
 * echarts 按需引入——只打包实际用到的图表和组件，
 * 避免全量导入导致 bundle 膨胀到 1041KB。
 */
import * as echarts from "echarts/core";
import { BarChart, LineChart, PieChart } from "echarts/charts";
import {
  GridComponent,
  TooltipComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  CanvasRenderer,
]);

export default echarts;
export type { ECharts } from "echarts/core";
