package de.yanwittmann.matheval.operator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Operators {

    private final List<Operator> operators = new ArrayList<>();

    public Operators() {
        add(Operator.make("++", 150, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("--", 150, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("++", 150, true, false, (arguments) -> {
            return null;
        }));
        add(Operator.make("--", 150, true, false, (arguments) -> {
            return null;
        }));
        add(Operator.make("+", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("+", 140, true, true, (arguments) -> {
            return null;
        }));
    }

    public void add(Operator operator) {
        operators.add(operator);
        operators.sort(
                Comparator
                        .comparing(Operator::getPrecedence)
                        .thenComparing(o -> o.getSymbol().length())
                        .thenComparing(o -> o.isLeftAssociative() ? 0 : 1)
                        .reversed()
        );

        operators.stream().map(Operator::toString).forEach(System.out::println);
        System.out.println();
    }

    public void remove(Operator operator) {
        operators.remove(operator);
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public List<Operator> getOperator(String symbol) {
        final List<Operator> result = new ArrayList<>();

        for (Operator operator : operators) {
            if (operator.getSymbol().equals(symbol)) {
                result.add(operator);
            }
        }

        return result;
    }
}
