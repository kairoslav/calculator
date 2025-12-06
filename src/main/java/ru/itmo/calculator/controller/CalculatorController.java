package ru.itmo.calculator.controller;

import org.springframework.web.bind.annotation.RestController;
import ru.itmo.calculator.execution.InstructionExecutionFacade;
import ru.itmo.calculator.openapi.api.CalculatorApi;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequestDto;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponseDto;

@RestController
public class CalculatorController implements CalculatorApi {

    private final InstructionExecutionFacade executionFacade;

    public CalculatorController(InstructionExecutionFacade executionFacade) {
        this.executionFacade = executionFacade;
    }

    @Override
    public ExecuteProgramResponseDto executeProgram(ExecuteProgramRequestDto executeProgramRequestDto) {
        return executionFacade.execute(executeProgramRequestDto);
    }
}
