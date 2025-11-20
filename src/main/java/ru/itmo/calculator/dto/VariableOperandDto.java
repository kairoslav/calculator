package ru.itmo.calculator.dto;

/**
 * Operand DTO that references another variable.
 */
public record VariableOperandDto(String name) implements OperandDto {
    public VariableOperandDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Variable name must be provided");
        }
    }
}
