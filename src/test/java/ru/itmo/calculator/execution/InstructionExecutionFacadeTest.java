package ru.itmo.calculator.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.itmo.calculator.converter.CalculatorApiConverter;
import ru.itmo.calculator.converter.GrpcInstructionConverter;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.dto.PrintInstruction;
import ru.itmo.calculator.dto.PrintResult;
import ru.itmo.calculator.generated.grpc.ExecuteProgramRequest;
import ru.itmo.calculator.generated.grpc.ExecuteProgramResponse;
import ru.itmo.calculator.generated.grpc.PrintedValue;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequestDto;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponseDto;
import ru.itmo.calculator.openapi.model.PrintedValueDto;

class InstructionExecutionFacadeTest {

    private final InstructionExecutionService executionService = org.mockito.Mockito.mock(InstructionExecutionService.class);
    private final CalculatorApiConverter apiConverter = org.mockito.Mockito.mock(CalculatorApiConverter.class);
    private final GrpcInstructionConverter grpcConverter = org.mockito.Mockito.mock(GrpcInstructionConverter.class);
    private final InstructionExecutionFacade facade =
            new InstructionExecutionFacade(executionService, apiConverter, grpcConverter);

    @Test
    void executesRestRequestWithSingleConversion() {
        ExecuteProgramRequestDto requestDto = new ExecuteProgramRequestDto();
        List<Instruction> instructions = List.of(new PrintInstruction("x"));
        List<PrintResult> results = List.of(new PrintResult("x", 7));
        List<PrintedValueDto> responseItems = List.of(new PrintedValueDto().var("x").value(7L));

        when(apiConverter.toDomainInstructions(requestDto)).thenReturn(instructions);
        when(executionService.execute(instructions)).thenReturn(results);
        when(apiConverter.toPrintedValues(results)).thenReturn(responseItems);

        ExecuteProgramResponseDto response = facade.execute(requestDto);

        ArgumentCaptor<List<Instruction>> instructionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(executionService).execute(instructionsCaptor.capture());
        assertEquals(instructions, instructionsCaptor.getValue());
        assertEquals(responseItems, response.getItems());
    }

    @Test
    void executesGrpcRequestWithSingleConversion() {
        ExecuteProgramRequest request = ExecuteProgramRequest.getDefaultInstance();
        List<Instruction> instructions = List.of(new PrintInstruction("y"));
        List<PrintResult> results = List.of(new PrintResult("y", 9));
        ExecuteProgramResponse response =
                ExecuteProgramResponse.newBuilder()
                        .addItems(PrintedValue.newBuilder().setVar("y").setValue(9).build())
                        .build();

        when(grpcConverter.toDomainInstructions(request)).thenReturn(instructions);
        when(executionService.execute(instructions)).thenReturn(results);
        when(grpcConverter.toResponse(results)).thenReturn(response);

        ExecuteProgramResponse actual = facade.execute(request);

        ArgumentCaptor<List<Instruction>> instructionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(executionService).execute(instructionsCaptor.capture());
        assertEquals(instructions, instructionsCaptor.getValue());
        assertEquals(response, actual);
    }
}
