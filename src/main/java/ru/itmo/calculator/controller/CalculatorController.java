package ru.itmo.calculator.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.execution.InstructionExecutionService;
import ru.itmo.calculator.converter.CalculatorApiConverter;
import ru.itmo.calculator.openapi.api.CalculatorApi;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequestDto;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponseDto;
import ru.itmo.calculator.openapi.model.PrintedValueDto;

@RestController
public class CalculatorController implements CalculatorApi {

    private final InstructionExecutionService executionService;
    private final CalculatorApiConverter converter;

    public CalculatorController(InstructionExecutionService executionService, CalculatorApiConverter converter) {
        this.executionService = executionService;
        this.converter = converter;
    }

    @Override
    public ExecuteProgramResponseDto executeProgram(ExecuteProgramRequestDto executeProgramRequestDto) {
        List<Instruction> domainInstructions = converter.toDomainInstructions(executeProgramRequestDto);
        List<PrintedValueDto> items =
                converter.toPrintedValues(executionService.execute(domainInstructions));
        return new ExecuteProgramResponseDto().items(items);
    }
}
