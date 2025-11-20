package ru.itmo.calculator.execution;

/**
 * Numeric literal operand.
 */
public record LiteralOperand(long value) implements Operand {
}
