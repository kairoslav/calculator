package ru.itmo.calculator.execution;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.itmo.calculator.converter.CalculatorApiConverter;
import ru.itmo.calculator.converter.GrpcInstructionConverter;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.dto.PrintResult;
import ru.itmo.calculator.generated.grpc.ExecuteProgramRequest;
import ru.itmo.calculator.generated.grpc.ExecuteProgramResponse;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequestDto;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponseDto;
import ru.itmo.calculator.openapi.model.PrintedValueDto;

/**
 * Orchestrates conversion from transport-layer requests to domain instructions and back.
 */
@Service
public class InstructionExecutionFacade {

    private final InstructionExecutionService executionService;
    private final CalculatorApiConverter apiConverter;
    private final GrpcInstructionConverter grpcConverter;

    public InstructionExecutionFacade(
            InstructionExecutionService executionService,
            CalculatorApiConverter apiConverter,
            GrpcInstructionConverter grpcConverter) {
        this.executionService = executionService;
        this.apiConverter = apiConverter;
        this.grpcConverter = grpcConverter;
    }

    public ExecuteProgramResponseDto execute(ExecuteProgramRequestDto requestDto) {
        List<Instruction> instructions = apiConverter.toDomainInstructions(requestDto);
        List<PrintResult> results = executionService.execute(instructions);
        List<PrintedValueDto> items = apiConverter.toPrintedValues(results);
        return new ExecuteProgramResponseDto().items(items);
    }

    public ExecuteProgramResponse execute(ExecuteProgramRequest request) {
        List<Instruction> instructions = grpcConverter.toDomainInstructions(request);
        List<PrintResult> results = executionService.execute(instructions);
        return grpcConverter.toResponse(results);
    }
}
