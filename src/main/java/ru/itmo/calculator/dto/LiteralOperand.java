package ru.itmo.calculator.dto;

/**
 * Numeric literal operand.
 */
public record LiteralOperand(long value) implements Operand {
}
