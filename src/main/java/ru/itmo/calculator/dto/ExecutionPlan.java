package ru.itmo.calculator.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ExecutionPlan(
        Set<String> requiredVariables,
        Map<String, CalcInstruction> calculations,
        List<String> executionOrder) {
}