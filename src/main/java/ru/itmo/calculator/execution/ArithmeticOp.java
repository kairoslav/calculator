package ru.itmo.calculator.execution;

/**
 * Supported arithmetic operations.
 */
public enum ArithmeticOp {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*");

    private final String symbol;

    ArithmeticOp(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static ArithmeticOp fromSymbol(String raw) {
        for (ArithmeticOp op : values()) {
            if (op.symbol.equals(raw)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unsupported operation: " + raw);
    }
}
