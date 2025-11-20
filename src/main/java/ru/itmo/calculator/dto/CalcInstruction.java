package ru.itmo.calculator.dto;

/**
 * Instruction that computes a value and stores it into a variable.
 */
public record CalcInstruction(String var, ArithmeticOp op, Operand left, Operand right) implements Instruction {
    public CalcInstruction {
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
