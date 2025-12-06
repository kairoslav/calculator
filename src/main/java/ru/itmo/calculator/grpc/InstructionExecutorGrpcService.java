package ru.itmo.calculator.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import ru.itmo.calculator.execution.InstructionExecutionFacade;
import ru.itmo.calculator.generated.grpc.ExecuteProgramRequest;
import ru.itmo.calculator.generated.grpc.ExecuteProgramResponse;
import ru.itmo.calculator.generated.grpc.InstructionExecutorGrpc;

@GrpcService
public class InstructionExecutorGrpcService extends InstructionExecutorGrpc.InstructionExecutorImplBase {

    private final InstructionExecutionFacade executionFacade;

    public InstructionExecutorGrpcService(InstructionExecutionFacade executionFacade) {
        this.executionFacade = executionFacade;
    }

    @Override
    public void execute(ExecuteProgramRequest request, StreamObserver<ExecuteProgramResponse> responseObserver) {
        try {
            responseObserver.onNext(executionFacade.execute(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }
}
