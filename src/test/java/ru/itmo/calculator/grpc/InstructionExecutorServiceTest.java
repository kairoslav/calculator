package ru.itmo.calculator.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.itmo.calculator.dto.ArithmeticOpDto;
import ru.itmo.calculator.dto.CalcInstructionDto;
import ru.itmo.calculator.dto.InstructionDto;
import ru.itmo.calculator.dto.LiteralOperandDto;
import ru.itmo.calculator.dto.PrintInstructionDto;
import ru.itmo.calculator.dto.PrintResultDto;
import ru.itmo.calculator.execution.InstructionExecutionService;
import ru.itmo.calculator.generated.grpc.ExecuteProgramRequest;
import ru.itmo.calculator.generated.grpc.ExecuteProgramResponse;
import ru.itmo.calculator.generated.grpc.Operation;
import ru.itmo.calculator.grpc.GrpcInstructionConverter;
import ru.itmo.calculator.grpc.InstructionExecutorService;

class InstructionExecutorServiceTest {

    @Test
    void executesProgramUsingExecutionService() {
        InstructionExecutionService executionService = mock(InstructionExecutionService.class);
        when(executionService.execute(anyList())).thenReturn(List.of(new PrintResultDto("x", 3)));

        InstructionExecutorService service =
                new InstructionExecutorService(executionService, new GrpcInstructionConverter());
        RecordingStreamObserver<ExecuteProgramResponse> observer = new RecordingStreamObserver<>();

        service.execute(buildRequest(), observer);

        ArgumentCaptor<List<InstructionDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(executionService).execute(captor.capture());
        List<InstructionDto> domainInstructions = captor.getValue();
        assertEquals(2, domainInstructions.size());

        CalcInstructionDto calc = (CalcInstructionDto) domainInstructions.get(0);
        assertEquals("x", calc.var());
        assertEquals(ArithmeticOpDto.ADD, calc.op());
        assertEquals(new LiteralOperandDto(1), calc.left());
        assertEquals(new LiteralOperandDto(2), calc.right());

        PrintInstructionDto print = (PrintInstructionDto) domainInstructions.get(1);
        assertEquals("x", print.var());

        assertTrue(observer.completed);
        assertNull(observer.error);
        assertEquals(1, observer.values.size());
        ExecuteProgramResponse response = observer.values.getFirst();
        assertEquals(1, response.getItemsCount());
        assertEquals("x", response.getItems(0).getVar());
        assertEquals(3, response.getItems(0).getValue());
    }

    @Test
    void wrapsExecutionErrorsIntoInvalidArgumentStatus() {
        InstructionExecutionService executionService = mock(InstructionExecutionService.class);
        when(executionService.execute(anyList())).thenThrow(new IllegalArgumentException("boom"));

        InstructionExecutorService service =
                new InstructionExecutorService(executionService, new GrpcInstructionConverter());
        RecordingStreamObserver<ExecuteProgramResponse> observer = new RecordingStreamObserver<>();

        service.execute(buildRequest(), observer);

        assertFalse(observer.completed);
        assertTrue(observer.values.isEmpty());
        assertNotNull(observer.error);

        StatusRuntimeException status = (StatusRuntimeException) observer.error;
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getStatus().getCode());
        assertEquals("boom", status.getStatus().getDescription());
    }

    private ExecuteProgramRequest buildRequest() {
        return ExecuteProgramRequest.newBuilder()
                .addInstructions(
                        ru.itmo.calculator.generated.grpc.InstructionDto.newBuilder()
                                .setCalc(
                                        ru.itmo.calculator.generated.grpc.CalcInstructionDto.newBuilder()
                                                .setVar("x")
                                                .setOp(Operation.OPERATION_ADD)
                                                .setLeft(ru.itmo.calculator.generated.grpc.OperandDto.newBuilder().setLiteral(1).build())
                                                .setRight(ru.itmo.calculator.generated.grpc.OperandDto.newBuilder().setLiteral(2).build())
                                                .build())
                                .build())
                .addInstructions(
                        ru.itmo.calculator.generated.grpc.InstructionDto.newBuilder()
                                .setPrint(ru.itmo.calculator.generated.grpc.PrintInstructionDto.newBuilder().setVar("x").build())
                                .build())
                .build();
    }

    private static final class RecordingStreamObserver<T> implements StreamObserver<T> {
        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
