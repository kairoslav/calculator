package ru.itmo.calculator.grpc;

import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class InstructionExecutorService extends InstructionExecutorGrpc.InstructionExecutorImplBase {

    @Override
    public void execute(ExecuteProgramRequest request, StreamObserver<ExecuteProgramResponse> responseObserver) {
        ExecuteProgramResponse response = ExecuteProgramResponse.newBuilder()
            .addItems(PrintedValue.newBuilder()
                .setVar("result")
                .setValue(42)
                .build())
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
