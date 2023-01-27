package de.yanwittmann.menter.interpreter.core;

import de.yanwittmann.menter.interpreter.MenterDebugger;
import de.yanwittmann.menter.interpreter.structure.EvaluationContext;
import de.yanwittmann.menter.interpreter.structure.EvaluationContextLocalInformation;
import de.yanwittmann.menter.interpreter.structure.GlobalContext;
import de.yanwittmann.menter.interpreter.structure.value.CustomType;
import de.yanwittmann.menter.interpreter.structure.value.PrimitiveValueType;
import de.yanwittmann.menter.interpreter.structure.value.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreModuleCmdPlot {

    static {
        EvaluationContext.registerNativeFunction("cmdplot.mtr", "plot", CoreModuleCmdPlot::plot);
        EvaluationContext.registerNativeFunction("cmdplot.mtr", "space", CoreModuleCmdPlot::space);
    }

    public static Value plot(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> arguments) {
        final String[][] parameterCombinations = {
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.OBJECT.getType()},
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.FUNCTION.getType()},
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.NATIVE_FUNCTION.getType()},
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.REFLECTIVE_FUNCTION.getType()},
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.NUMBER.getType(), PrimitiveValueType.NUMBER.getType()},
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.FUNCTION.getType(), PrimitiveValueType.NUMBER.getType(), PrimitiveValueType.NUMBER.getType()},
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.NATIVE_FUNCTION.getType(), PrimitiveValueType.NUMBER.getType(), PrimitiveValueType.NUMBER.getType()},
                {PrimitiveValueType.OBJECT.getType(), PrimitiveValueType.REFLECTIVE_FUNCTION.getType(), PrimitiveValueType.NUMBER.getType(), PrimitiveValueType.NUMBER.getType()},
        };
        final int parameterCombination = CustomType.checkParameterCombination(arguments, parameterCombinations);

        final int width;
        final int height;
        final Value x, y;

        switch (parameterCombination) {
            case 0:
                x = arguments.get(0);
                y = arguments.get(1);
                width = 120;
                height = 30;
                break;
            case 1:
            case 2:
            case 3:
                x = arguments.get(0);
                y = generateYValues(context, localInformation, arguments, x);
                width = 120;
                height = 30;
                break;
            case 4:
                x = arguments.get(0);
                y = arguments.get(1);
                width = ((BigDecimal) arguments.get(2).getValue()).intValue();
                height = ((BigDecimal) arguments.get(3).getValue()).intValue();
                break;
            case 5:
            case 6:
            case 7:
                x = arguments.get(0);
                y = generateYValues(context, localInformation, arguments, x);
                width = ((BigDecimal) arguments.get(2).getValue()).intValue();
                height = ((BigDecimal) arguments.get(3).getValue()).intValue();
                break;
            default:
                throw CustomType.invalidParameterCombinationException("common", "plot", arguments, parameterCombinations);
        }

        final BigDecimal maxX = getMaxValue(x);
        final BigDecimal minX = getMinValue(x);
        final BigDecimal maxY = getMaxValue(y);
        final BigDecimal minY = getMinValue(y);

        final double xScale = (maxX.subtract(minX)).doubleValue() / width;
        final double yScale = (maxY.subtract(minY)).doubleValue() / height;

        final char[][] plot = new char[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                plot[i][j] = ' ';
            }
        }

        // draw the grid lines
        final int xAxisHeight = getAxisPosition(minY, yScale, height);
        for (int i = 0; i < width; i++) {
            setPlotValue(plot, i, xAxisHeight, i == width - 1 ? '>' : '-');
        }

        final int yAxisWidth = getAxisPosition(minX, xScale, width);
        for (int i = 0; i < height; i++) {
            setPlotValue(plot, yAxisWidth, i, i == height - 1 ? '^' : '|');
        }

        final BigDecimal[] xValues = x.getMap().values().stream().map(v -> (BigDecimal) v.getValue()).toArray(BigDecimal[]::new);
        final BigDecimal[] yValues = y.getMap().values().stream().map(v -> (BigDecimal) v.getValue()).toArray(BigDecimal[]::new);

        for (int i = 0; i < xValues.length; i++) {
            final int xIndex = getScaledIndex(minX, xScale, xValues[i], height);
            final int yIndex = getScaledIndex(minY, yScale, yValues[i], height);
            final int previousYIndex = i == 0 ? yIndex : getScaledIndex(minY, yScale, yValues[i - 1], height);

            final char gradient = yIndex > previousYIndex ? '/' : yIndex < previousYIndex ? '\\' : 'x';

            setPlotValue(plot, xIndex, yIndex, gradient);
        }

        for (int i = 0; i < width; i += 15) {
            final String tickValue = getTickValue(minX, xScale, i);
            if (!tickValue.equals("0")) {
                setPlotValue(plot, i, xAxisHeight, '|');
                if (xAxisHeight + 1 < height) {
                    setPlotValue(plot, i, xAxisHeight + 1, tickValue);
                } else {
                    setPlotValue(plot, i, xAxisHeight - 1, tickValue);
                }
            }
        }

        for (int i = 0; i < height; i += 5) {
            final String tickValue = getTickValue(minY, yScale, i);
            if (!tickValue.equals("0")) {
                if (yAxisWidth + 1 < width) {
                    setPlotValue(plot, yAxisWidth + 1, i, " " + tickValue);
                } else {
                    setPlotValue(plot, yAxisWidth - 1, i, " " + tickValue);
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

    private static Value generateYValues(GlobalContext context, EvaluationContextLocalInformation localInformation, List<Value> arguments, Value x) {
        final Value y;
        final List<Value> mappedValues = new ArrayList<>();
        for (Value value : x.getMap().values()) {
            mappedValues.add(context.evaluateFunction("cmdplot.plot.eval", arguments.get(1), context, localInformation, value));
        }
        y = new Value(mappedValues);
        return y;
    }

    public static Value space(List<Value> parameters) {
        // find a fitting step size for the range and then use CoreModuleCommon.range to generate the values
        final BigDecimal min = (BigDecimal) parameters.get(0).getValue();
        final BigDecimal max = (BigDecimal) parameters.get(1).getValue();
        final BigDecimal range = max.subtract(min);

        // defaults to 120 values
        final int targetValueCount = parameters.size() == 3 ? ((BigDecimal) parameters.get(2).getValue()).intValue() : 120;

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return CoreModuleMath.range(Arrays.asList(new Value(parameters.get(0)), new Value(parameters.get(1)), new Value(BigDecimal.ONE)));
        }

        final BigDecimal stepSize = range.divide(BigDecimal.valueOf(targetValueCount), 2, RoundingMode.HALF_UP);

        return CoreModuleMath.range(Arrays.asList(new Value(parameters.get(0)), new Value(parameters.get(1)), new Value(stepSize)));
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

    private static int getScaledIndex(BigDecimal min, double scale, BigDecimal value, int height) {
        final BigDecimal subtract = value.subtract(min);
        if (subtract.compareTo(BigDecimal.ZERO) == 0) {
            return 1;
        }
        final BigDecimal scaledValue = subtract.divide(BigDecimal.valueOf(scale), RoundingMode.HALF_EVEN);
        final int intValue = scaledValue.intValue();
        if (intValue == height) {
            return intValue - 1;
        } else if (intValue == -1) {
            return 0;
        }
        return intValue;
    }

    private static void setPlotValue(char[][] plot, int x, int y, char value) {
        if (x >= 0 && x < plot.length && y >= 0 && y < plot[0].length) {
            plot[x][y] = value;
        }
    }

    private static void setPlotValue(char[][] plot, int x, int y, String value) {
        if (x >= 0 && x < plot.length && y >= 0 && y < plot[0].length) {
            for (int i = 0; i < value.length(); i++) {
                plot[x + i][y] = value.charAt(i);
            }
        }
    }

    private static BigDecimal getMinValue(Value list) {
        return list.getMap().values().stream().map(v -> (BigDecimal) v.getValue()).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getMaxValue(Value list) {
        return list.getMap().values().stream().map(v -> (BigDecimal) v.getValue()).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }
}
