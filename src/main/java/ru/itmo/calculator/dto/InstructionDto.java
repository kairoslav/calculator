package ru.itmo.calculator.dto;

/**
 * Marker interface for supported instructions.
 */
public sealed interface InstructionDto permits CalcInstructionDto, PrintInstructionDto {
}
