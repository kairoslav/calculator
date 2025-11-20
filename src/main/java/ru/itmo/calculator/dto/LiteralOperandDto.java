package ru.itmo.calculator.dto;

/**
 * Numeric literal operand.
 */
public record LiteralOperandDto(long value) implements OperandDto {
}
