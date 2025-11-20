package ru.itmo.calculator.dto;

/**
 * Instruction DTO that requests printing a variable value.
 */
public record PrintInstruction(String var) implements Instruction {
    public PrintInstruction {
        if (var == null || var.isBlank()) {
            throw new IllegalArgumentException("Variable name must be provided");
        }
    }
}
