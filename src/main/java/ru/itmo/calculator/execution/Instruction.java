package ru.itmo.calculator.execution;

/**
 * Marker interface for supported instructions.
 */
public sealed interface Instruction permits CalcInstruction, PrintInstruction {
}
