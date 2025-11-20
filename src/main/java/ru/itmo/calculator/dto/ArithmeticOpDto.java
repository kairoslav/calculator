package ru.itmo.calculator.dto;

/**
 * Supported arithmetic operations.
 */
public enum ArithmeticOpDto {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*");

    private final String symbol;

    ArithmeticOpDto(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static ArithmeticOpDto fromSymbol(String raw) {
        for (ArithmeticOpDto op : values()) {
            if (op.symbol.equals(raw)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unsupported operation: " + raw);
    }
}
