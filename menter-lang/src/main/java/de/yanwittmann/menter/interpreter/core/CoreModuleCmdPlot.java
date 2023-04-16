package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.exceptions.MenterExecutionException;
import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.EvaluationContextLocalInformation;
import de.yanwittmann.menter.interpreter.structure.GlobalContext;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import de.yanwittmann.menter.operator.Operators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class CoreModuleCmdPlot {

    static {
        EvaluationContext.registerNativeFunction("cmdplot.mtr", "plot", CoreModuleCmdPlot::plot);
        EvaluationContext.registerNativeFunction("cmdplot.mtr", "table", CoreModuleCmdPlot::table);
    }

    public static Value plot(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> arguments) {
        if (arguments.size() < 2) {
            throw new MenterExecutionException("The plot function requires at least two arguments: [width] [height] [xRange] [yRange...] [function...]");
        }

        final boolean hasSpecifiedPlotSize = PrimitiveValueType.isType(arguments.get(0), PrimitiveValueType.NUMBER.getType()) &&
                                             PrimitiveValueType.isType(arguments.get(1), PrimitiveValueType.NUMBER.getType());
        final boolean hasInputXRange = PrimitiveValueType.isType(arguments.get(0), PrimitiveValueType.OBJECT.getType()) ||
                                       (hasSpecifiedPlotSize && PrimitiveValueType.isType(arguments.get(2), PrimitiveValueType.OBJECT.getType()));
        final boolean hasFunction = arguments.stream().anyMatch(value -> PrimitiveValueType.isType(value, PrimitiveValueType.FUNCTION.getType()));
        final boolean hasInputYRange = PrimitiveValueType.isType(arguments.get(arguments.size() - 1), PrimitiveValueType.OBJECT.getType());

        if (!hasInputXRange) {
            throw new MenterExecutionException("The first argument of the plot function must be an array of numbers if a function is specified.");
        }
        if (!hasFunction && !hasInputYRange) {
            throw new MenterExecutionException("The last argument of the plot function must be either an array of numbers or a function.");
        }


        final Value x = hasSpecifiedPlotSize ? arguments.get(2) : arguments.get(0);
        final List<Value> y = generateYValues(context, localInformation, x, arguments);


        final int width;
        final int height;
        if (hasSpecifiedPlotSize) {
            width = ((BigDecimal) arguments.get(0).getValue()).intValue();
            height = ((BigDecimal) arguments.get(1).getValue()).intValue();
        } else {
            width = 120;
            height = 30;
        }
        final int padding = 1;
        final int plotWidth = width - padding * 2;
        final int plotHeight = height - padding * 2;

        final BigDecimal maxX = getMaxValue(x);
        final BigDecimal minX = getMinValue(x);
        final BigDecimal maxY = getMaxValue(y);
        final BigDecimal minY = getMinValue(y);

        final double xScale = (maxX.subtract(minX)).doubleValue() / plotWidth;
        final double yScale = (maxY.subtract(minY)).doubleValue() / plotHeight;

        final String[][] plot = new String[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                plot[i][j] = " ";
            }
        }

        // draw the grid lines
        final int xAxisHeight = getAxisPosition(minY, yScale, plotHeight);
        for (int i = 0; i < width; i++) {
            setPlotValue(plot, i, xAxisHeight, i == width - 1 ? '>' : '-');
        }

        final int yAxisWidth = getAxisPosition(minX, xScale, plotWidth);
        for (int i = 0; i < height; i++) {
            setPlotValue(plot, yAxisWidth, i, i == height - 1 ? '^' : '|');
        }

        // draw the functions onto the plot
        final Integer[] xValues = x.getMap().values().stream()
                .map(v -> (BigDecimal) v.getValue())
                .map(v -> v == null ? null : getScaledIndex(minX, xScale, v))
                .toArray(Integer[]::new);

        final Integer[][] yValues = new Integer[y.size()][];
        for (int i = 0; i < y.size(); i++) {
            final Value yValue = y.get(i);
            yValues[i] = yValue.getMap().values().stream()
                    .map(v -> (BigDecimal) v.getValue())
                    .map(v -> v == null ? null : getScaledIndex(minY, yScale, v))
                    .toArray(Integer[]::new);
        }

        for (int i = 0; i < yValues.length; i++) {
            for (int j = 0; j < xValues.length; j++) {
                final Integer current = yValues[i][j];
                final Integer previous = yValues[i][j == 0 ? 0 : j - 1];

                if (current == null) continue;

                final char gradient = previous == null ? 'x' : (current > previous ? '/' : current < previous ? '\\' : 'x');
                setPlotValue(plot, xValues[j], current, AnsiUtilities.colorize(gradient, getChartColorByIndex(i)), true);
            }
        }

        // find missing x values and interpolate them using "-"
        for (int i = 0; i < yValues.length; i++) {
            for (int j = 1; j < plotWidth - 1; j++) {
                final int currentIndex = j;
                final boolean isMissing = xValues.length == 0 || Arrays.stream(xValues).noneMatch(v -> v == currentIndex);

                if (isMissing) {
                    int previousIndex = currentIndex;
                    for (int k = currentIndex; k >= 0; k--) {
                        int finalK = k;
                        if (Arrays.stream(xValues).anyMatch(v -> v == finalK)) {
                            previousIndex = k;
                            break;
                        }
                    }
                    int nextIndex = currentIndex;
                    for (int k = currentIndex; k <= plotWidth; k++) {
                        int finalK = k;
                        if (Arrays.stream(xValues).anyMatch(v -> v == finalK)) {
                            nextIndex = k;
                            break;
                        }
                    }

                    Integer previousY = null;
                    for (int k = 0; k < xValues.length; k++) {
                        if (xValues[k] == previousIndex) {
                            previousY = yValues[i][k];
                            break;
                        }
                    }
                    Integer nextY = null;
                    for (int k = 0; k < xValues.length; k++) {
                        if (xValues[k] == nextIndex) {
                            nextY = yValues[i][k];
                            break;
                        }
                    }

                    if (previousY != null && nextY != null) {
                        final int yDiff = nextY - previousY;
                        final int xDiff = nextIndex - previousIndex;
                        final int interpolated = previousY + (yDiff * (currentIndex - previousIndex)) / xDiff;
                        setPlotValue(plot, currentIndex, interpolated, AnsiUtilities.colorize('-', getChartColorByIndex(i)), true);
                    } else if (previousY != null) {
                        setPlotValue(plot, currentIndex, previousY, AnsiUtilities.colorize('-', getChartColorByIndex(i)), true);
                    } else if (nextY != null) {
                        setPlotValue(plot, currentIndex, nextY, AnsiUtilities.colorize('-', getChartColorByIndex(i)), true);
                    }
                }
            }
        }

        for (int i = 0; i < width; i += 15) {
            final String tickValue = getTickValue(minX, xScale, i);
            if (!tickValue.equals("0")) {
                setPlotValue(plot, i, xAxisHeight, '|');
                if (xAxisHeight + 1 < height) {
                    setPlotValue(plot, i, xAxisHeight + 1, tickValue, false);
                } else {
                    setPlotValue(plot, i, xAxisHeight - 1, tickValue, false);
                }
            }
        }

        for (int i = 0; i < height; i += 5) {
            final String tickValue = getTickValue(minY, yScale, i);
            if (!tickValue.equals("0")) {
                if (yAxisWidth + 1 < width) {
                    setPlotValue(plot, yAxisWidth + 1, i, " " + tickValue, false);
                } else {
                    setPlotValue(plot, yAxisWidth - 1, i, " " + tickValue, false);
                }
            }
        }

        for (int i = height - 1; i >= 0; i--) {
            for (int j = 0; j < width; j++) {
                MenterDebugger.printer.print(plot[j][i]);
            }
            MenterDebugger.printer.println();
        }

        return Value.empty();
    }

    private static List<Value> generateYValues(GlobalContext context, EvaluationContextLocalInformation localInformation, Value xValues, List<Value> arguments) {
        final List<Value> yValues = new ArrayList<>();

        boolean isFirst = true;
        for (int i = 0; i < arguments.size(); i++) {
            final Value currentArgument = arguments.get(i);

            if (PrimitiveValueType.isType(currentArgument, PrimitiveValueType.FUNCTION)) {
                if (isFirst) {
                    isFirst = false;
                }
                final List<Value> y = new ArrayList<>();
                for (Value x : xValues.getMap().values()) {
                    final Value result;
                    try {
                        result = context.evaluateFunction("cmdplot.plot.eval", currentArgument, context, localInformation, x);
                    } catch (Exception e) {
                        y.add(Value.empty());
                        continue;
                    }

                    if (!PrimitiveValueType.isType(result, PrimitiveValueType.NUMBER)) {
                        throw new MenterExecutionException("The plot function can only plot functions that return numbers.");
                    }
                    y.add(result);
                }
                yValues.add(new Value(y));
            } else if (PrimitiveValueType.isType(currentArgument, PrimitiveValueType.OBJECT)) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }
                yValues.add(currentArgument);
            }
        }

        return yValues;
    }

    private static int getAxisPosition(BigDecimal min, double scale, int max) {
        final int xAxisHeight = (int) ((BigDecimal.ZERO.subtract(min)).doubleValue() / scale);
        if (xAxisHeight < 0) {
            return 0;
        } else if (xAxisHeight >= max) {
            return max - 1;
        }
        return xAxisHeight;
    }

    private static String getTickValue(BigDecimal min, double scale, int i) {
        final String tick = String.valueOf(min.add(BigDecimal.valueOf(i * scale)));
        int maxDecimalPlaces = 2;
        if (tick.contains(".")) {
            int indexOfDot = tick.indexOf('.');
            final String replacedTick = tick.substring(0, indexOfDot + (tick.length() - indexOfDot > maxDecimalPlaces ? maxDecimalPlaces + 1 : tick.length() - indexOfDot));
            if (replacedTick.endsWith(".0")) {
                return replacedTick.substring(0, replacedTick.length() - 2);
            } else {
                return replacedTick;
            }
        }
        return tick;
    }

    private static int getScaledIndex(BigDecimal min, double scale, BigDecimal value) {
        final BigDecimal subtract = value.subtract(min);
        if (subtract.compareTo(BigDecimal.ZERO) == 0) {
            return 1;
        }
        final BigDecimal scaledValue = subtract.divide(BigDecimal.valueOf(scale), Operators.getBigDecimalDivisionScale(), RoundingMode.HALF_EVEN);
        return scaledValue.intValue();
    }

    private static void setPlotValue(String[][] plot, int x, int y, char value) {
        if (x >= 0 && x < plot.length && y >= 0 && y < plot[0].length) {
            plot[x][y] = String.valueOf(value);
        }
    }

    private static void setPlotValue(String[][] plot, int x, int y, String value, boolean inOneCell) {
        if (x >= 0 && x < plot.length && y >= 0 && y < plot[0].length) {
            for (int i = 0; i < value.length(); i++) {
                if (x + i >= plot.length) {
                    break;
                }
                if (inOneCell) {
                    plot[x + i][y] = value;
                    break;
                } else {
                    plot[x + i][y] = String.valueOf(value.charAt(i));
                }
            }
        }
    }

    private static BigDecimal getMinValue(Value list) {
        return list.getMap().values().stream()
                .filter(v -> !v.isEmpty())
                .map(v -> (BigDecimal) v.getValue())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getMaxValue(Value list) {
        return list.getMap().values().stream()
                .filter(v -> !v.isEmpty())
                .map(v -> (BigDecimal) v.getValue())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getMaxValue(List<Value> lists) {
        return lists.stream()
                .map(CoreModuleCmdPlot::getMaxValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getMinValue(List<Value> lists) {
        return lists.stream()
                .map(CoreModuleCmdPlot::getMinValue)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static final String[] CHART_COLORS = {
            AnsiUtilities.ANSI_CYAN,
            AnsiUtilities.ANSI_GREEN,
            AnsiUtilities.ANSI_YELLOW,
            AnsiUtilities.ANSI_RED,
            AnsiUtilities.ANSI_PURPLE,
            AnsiUtilities.ANSI_BLUE,
            AnsiUtilities.ANSI_WHITE,
    };

    private static String getChartColorByIndex(int index) {
        return CHART_COLORS[index % CHART_COLORS.length];
    }

    private static Value table(List<Value> arguments) {
        if (arguments.size() == 0 || arguments.size() > 2) {
            throw new MenterExecutionException("The table function expects exactly one or two arguments: [index: boolean]? [list of maps]");
        }

        final boolean showIndex = arguments.size() == 2 && arguments.get(0).isTrue();

        final List<String> header = new ArrayList<>();
        final List<List<String>> rows = new ArrayList<>();

        if (arguments.get(arguments.size() - 1).isMapAnArray()) {
            final Value inputMapValue = arguments.get(arguments.size() - 1);
            final List<String> keys = inputMapValue.getMap().values().stream()
                    .filter(v -> !v.isEmpty())
                    .map(Value::getMap)
                    .flatMap(m -> m.keySet().stream())
                    .distinct()
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            if (showIndex) header.add("");
            header.addAll(keys);

            rows.add(header);

            for (int i = 0; i < inputMapValue.getMap().size(); i++) {
                final List<String> row = new ArrayList<>();
                if (showIndex) row.add(String.valueOf(i));
                for (String key : keys) {
                    final Value value = inputMapValue.getMap().get(new BigDecimal(i)).getMap().get(key);
                    if (value == null) {
                        row.add("");
                    } else {
                        row.add(value.toDisplayString());
                    }
                }
                rows.add(row);
            }
        } else {
            // {Mira: 19.53, Thomas: [15.76, 34.22], John: 19.71, Viola: 20.33, Yan: [20.12, 41.32], Daniel: 15.92}
            // should become:
            //  Key    | 0 | 1
            //  -------+----------
            //  Daniel | 20.47    |
            //  John   | 19.27    |
            //  Yan    | 17.43    | 41.32
            //  Thomas | 1 | 34.22
            //  Viola  | 20.6     |
            //  Mira   | 18.46  |

            final Value inputMapValue = arguments.get(arguments.size() - 1);
            final List<String> keys = inputMapValue.getMap().keySet().stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            if (showIndex) header.add("");
            header.add("Key");

            // dynamically determine the number of columns
            final int maxColumnCount = inputMapValue.getMap().values().stream()
                    .filter(v -> v.isMapAnArray())
                    .map(Value::getMap)
                    .map(Map::size)
                    .max(Integer::compareTo)
                    .orElse(1);
            for (int i = 0; i < maxColumnCount; i++) {
                header.add(String.valueOf(i));
            }

            rows.add(header);

            for (String key : keys) {
                final List<String> row = new ArrayList<>();
                if (showIndex) row.add("");
                row.add(key);
                final Value value = inputMapValue.getMap().get(key);
                if (value == null || value.isEmpty()) {
                    for (int i = 0; i < maxColumnCount; i++) {
                        row.add("");
                    }
                } else {
                    if (value.isMapAnArray()) {
                        for (int i = 0; i < maxColumnCount; i++) {
                            final Value v = value.getMap().get(new BigDecimal(i));
                            if (v == null || v.isEmpty()) {
                                row.add("");
                            } else {
                                row.add(v.toDisplayString());
                            }
                        }
                    } else {
                        row.add(value.toDisplayString());
                        for (int i = 1; i < maxColumnCount; i++) {
                            row.add("");
                        }
                    }
                }
                rows.add(row);
            }
        }

        // generate table from rows

        final List<Integer> columnWidths = new ArrayList<>();
        for (int i = 0; i < header.size(); i++) {
            int finalI = i;
            final int max = rows.stream()
                    .map(l -> l.get(finalI))
                    .map(String::length)
                    .max(Integer::compareTo)
                    .orElse(0);
            columnWidths.add(max);
        }

        final StringBuilder sb = new StringBuilder();
        for (int j = 0; j < rows.size(); j++) {
            final List<String> row = rows.get(j);
            final StringJoiner line = new StringJoiner(" | ");

            for (int i = 0; i < row.size(); i++) {
                final String value = row.get(i);
                final int width = columnWidths.get(i);
                line.add(String.format("%-" + width + "s", value));
            }

            sb.append(line);
            sb.append("\n");

            if (j == 0) {
                sb.append(columnWidths.stream()
                        .map(columnWidth -> String.join("", Collections.nCopies(columnWidth, "-")))
                        .collect(Collectors.joining("-+-")));
                sb.append("\n");
            }
        }

        MenterDebugger.printer.println(sb);
        return Value.empty();
    }
}
