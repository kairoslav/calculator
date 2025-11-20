package ru.itmo.calculator.execution;

/**
 * Operand used in calculations.
 */
public sealed interface Operand permits LiteralOperand, VariableOperand {
}
