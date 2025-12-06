package ru.itmo.calculator.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
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
import ru.itmo.calculator.execution.InstructionExecutionFacade;
import ru.itmo.calculator.generated.grpc.ExecuteProgramRequest;
import ru.itmo.calculator.generated.grpc.ExecuteProgramResponse;
import ru.itmo.calculator.generated.grpc.Operation;
import ru.itmo.calculator.generated.grpc.PrintedValue;

class InstructionExecutorGrpcServiceTest {

    @Test
    void executesProgramUsingExecutionFacade() {
        InstructionExecutionFacade executionFacade = mock(InstructionExecutionFacade.class);
        ExecuteProgramResponse response =
                ExecuteProgramResponse.newBuilder()
                        .addItems(PrintedValue.newBuilder().setVar("x").setValue(3).build())
                        .build();
        when(executionFacade.execute(buildRequest())).thenReturn(response);

        InstructionExecutorGrpcService service = new InstructionExecutorGrpcService(executionFacade);
        RecordingStreamObserver<ExecuteProgramResponse> observer = new RecordingStreamObserver<>();

        service.execute(buildRequest(), observer);

        ArgumentCaptor<ExecuteProgramRequest> captor = ArgumentCaptor.forClass(ExecuteProgramRequest.class);
        verify(executionFacade).execute(captor.capture());
        assertEquals(buildRequest(), captor.getValue());

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
        InstructionExecutionFacade executionFacade = mock(InstructionExecutionFacade.class);
        doThrow(new IllegalArgumentException("boom")).when(executionFacade).execute(buildRequest());

        InstructionExecutorGrpcService service = new InstructionExecutorGrpcService(executionFacade);
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
