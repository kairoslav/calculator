package ru.itmo.calculator.execution;

/**
 * Instruction that requests printing a variable value.
 */
public record PrintInstruction(String var) implements Instruction {
    public PrintInstruction {
        if (var == null || var.isBlank()) {
            throw new IllegalArgumentException("Variable name must be provided");
        }
    }
}
