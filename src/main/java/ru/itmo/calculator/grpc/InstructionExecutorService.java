package ru.itmo.calculator.grpc;

import java.util.List;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import ru.itmo.calculator.converter.GrpcInstructionConverter;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.dto.PrintResult;
import ru.itmo.calculator.execution.InstructionExecutionService;
import ru.itmo.calculator.generated.grpc.ExecuteProgramRequest;
import ru.itmo.calculator.generated.grpc.ExecuteProgramResponse;
import ru.itmo.calculator.generated.grpc.InstructionExecutorGrpc;

@GrpcService
public class InstructionExecutorService extends InstructionExecutorGrpc.InstructionExecutorImplBase {

    private final InstructionExecutionService executionService;
    private final GrpcInstructionConverter converter;

    public InstructionExecutorService(
            InstructionExecutionService executionService, GrpcInstructionConverter converter) {
        this.executionService = executionService;
        this.converter = converter;
    }

    @Override
    public void execute(ExecuteProgramRequest request, StreamObserver<ExecuteProgramResponse> responseObserver) {
        try {
            List<Instruction> instructions = converter.toDomainInstructions(request);
            List<PrintResult> results = executionService.execute(instructions);
            responseObserver.onNext(converter.toResponse(results));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }
}
