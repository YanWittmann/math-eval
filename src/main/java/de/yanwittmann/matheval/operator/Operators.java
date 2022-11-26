package de.yanwittmann.matheval.operator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Operators {

    private final List<Operator> operators = new ArrayList<>();

    public Operators() {
        // precedence values see https://introcs.cs.princeton.edu/java/11precedence/

        add(Operator.make("++", 150, true, false, (arguments) -> {
            return null;
        }));
        add(Operator.make("--", 150, true, false, (arguments) -> {
            return null;
        }));

        add(Operator.make("++", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("--", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("+", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("-", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("!", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("~", 140, false, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("~", 140, false, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("*", 120, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("/", 120, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("%", 120, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("+", 110, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("-", 110, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("<<", 100, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make(">>", 100, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make(">>>", 100, true, true, (arguments) -> {
            return null;
        })); // TODO: more than 2 character operators are not supported yet

        add(Operator.make("<", 90, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("<=", 90, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make(">", 90, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make(">=", 90, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("instanceof", 90, true, true, (arguments) -> {
            return null;
        })); // TODO: more than 2 character operators are not supported yet

        add(Operator.make("==", 80, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("!=", 80, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("&", 70, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("^", 60, true, true, (arguments) -> {
            return null;
        }));
        add(Operator.make("^^", 60, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("|", 50, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("&&", 40, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("||", 30, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("?", 20, true, true, (arguments) -> {
            return null;
        }));

        add(Operator.make("=", 10, true, true, (arguments) -> {
            return null;
        }, false));

        add(Operator.make("->", 0, true, true, (arguments) -> {
            return null;
        }));

        /*operators.stream().map(Operator::toString).forEach(System.out::println);
        System.out.println();*/
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
    }

    public void remove(Operator operator) {
        operators.remove(operator);
    }

    public List<Operator> getOperators() {
        return operators;
    }

    public List<Operator> getOperatorsSingleAssociative() {
        List<Operator> operators = new ArrayList<>();
        for (Operator operator : this.operators) {
            if (operator.isLeftAssociative() && !operator.isRightAssociative() ||
                    !operator.isLeftAssociative() && operator.isRightAssociative()) {
                operators.add(operator);
            }
        }
        return operators;
    }

    public List<Operator> getOperatorsDoubleAssociative() {
        List<Operator> operators = new ArrayList<>();
        for (Operator operator : this.operators) {
            if (operator.isLeftAssociative() && operator.isRightAssociative()) {
                operators.add(operator);
            }
        }
        return operators;
    }

    public List<Operator> findOperators(String symbol) {
        final List<Operator> result = new ArrayList<>();

        for (Operator operator : operators) {
            if (operator.getSymbol().equals(symbol)) {
                result.add(operator);
            }
        }

        return result;
    }

    public Operator findOperator(String symbol, boolean leftAssociative, boolean rightAssociative) {
        for (Operator operator : operators) {
            if (operator.getSymbol().equals(symbol) && operator.isLeftAssociative() == leftAssociative && operator.isRightAssociative() == rightAssociative) {
                return operator;
            }
        }
        return null;
    }
}
