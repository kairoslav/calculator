package ru.itmo.calculator.dto;

/**
 * Instruction DTO that requests printing a variable value.
 */
public record PrintInstructionDto(String var) implements InstructionDto {
    public PrintInstructionDto {
        if (var == null || var.isBlank()) {
            throw new IllegalArgumentException("Variable name must be provided");
        }
    }
}
