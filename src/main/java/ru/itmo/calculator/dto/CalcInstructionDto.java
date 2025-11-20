package ru.itmo.calculator.dto;

/**
 * Instruction DTO that computes a value and stores it into a variable.
 */
public record CalcInstructionDto(String var, ArithmeticOpDto op, OperandDto left, OperandDto right) implements InstructionDto {
    public CalcInstructionDto {
        if (var == null || var.isBlank()) {
            throw new IllegalArgumentException("Variable name must be provided");
        }
        if (op == null) {
            throw new IllegalArgumentException("Operation must be provided");
        }
        if (left == null || right == null) {
            throw new IllegalArgumentException("Operands must be provided");
        }
    }
}
