package ru.itmo.calculator.dto;

/**
 * Marker interface for supported instructions.
 */
public sealed interface Instruction permits CalcInstruction, PrintInstruction {
}
