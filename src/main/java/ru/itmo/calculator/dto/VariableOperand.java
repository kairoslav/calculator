package ru.itmo.calculator.dto;

/**
 * Operand DTO that references another variable.
 */
public record VariableOperand(String name) implements Operand {
    public VariableOperand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Variable name must be provided");
        }
    }
}
