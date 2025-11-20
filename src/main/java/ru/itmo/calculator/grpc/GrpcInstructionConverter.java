package ru.itmo.calculator.grpc;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import ru.itmo.calculator.dto.ArithmeticOpDto;
import ru.itmo.calculator.dto.CalcInstructionDto;
import ru.itmo.calculator.dto.InstructionDto;
import ru.itmo.calculator.dto.LiteralOperandDto;
import ru.itmo.calculator.dto.OperandDto;
import ru.itmo.calculator.dto.PrintInstructionDto;
import ru.itmo.calculator.dto.PrintResultDto;
import ru.itmo.calculator.dto.VariableOperandDto;
import ru.itmo.calculator.generated.grpc.ExecuteProgramRequest;
import ru.itmo.calculator.generated.grpc.ExecuteProgramResponse;
import ru.itmo.calculator.generated.grpc.Operation;
import ru.itmo.calculator.generated.grpc.PrintedValue;

@Component
public class GrpcInstructionConverter {

    public List<InstructionDto> toDomainInstructions(ExecuteProgramRequest request) {
        Objects.requireNonNull(request, "request");
        return request.getInstructionsList().stream().map(this::toDomainInstruction).toList();
    }

    public ExecuteProgramResponse toResponse(List<PrintResultDto> results) {
        ExecuteProgramResponse.Builder builder = ExecuteProgramResponse.newBuilder();

        for (PrintResultDto result : results) {
            builder.addItems(
                    PrintedValue.newBuilder()
                            .setVar(result.var())
                            .setValue(result.value())
                            .build());
        }

        return builder.build();
    }

    private InstructionDto toDomainInstruction(ru.itmo.calculator.generated.grpc.InstructionDto instruction) {
        return switch (instruction.getInstructionKindCase()) {
            case CALC -> toCalcInstruction(instruction.getCalc());
            case PRINT -> new PrintInstructionDto(instruction.getPrint().getVar());
            case INSTRUCTIONKIND_NOT_SET -> throw new IllegalArgumentException("InstructionDto kind is required");
        };
    }

    private CalcInstructionDto toCalcInstruction(ru.itmo.calculator.generated.grpc.CalcInstructionDto calc) {
        return new CalcInstructionDto(
                calc.getVar(), toArithmeticOp(calc.getOp()), toOperand(calc.getLeft()), toOperand(calc.getRight()));
    }

    private OperandDto toOperand(ru.itmo.calculator.generated.grpc.OperandDto operand) {
        return switch (operand.getValueCase()) {
            case LITERAL -> new LiteralOperandDto(operand.getLiteral());
            case VARIABLE -> new VariableOperandDto(operand.getVariable());
            case VALUE_NOT_SET -> throw new IllegalArgumentException("OperandDto value is required");
        };
    }

    private ArithmeticOpDto toArithmeticOp(Operation operation) {
        return switch (operation) {
            case OPERATION_ADD -> ArithmeticOpDto.ADD;
            case OPERATION_SUBTRACT -> ArithmeticOpDto.SUBTRACT;
            case OPERATION_MULTIPLY -> ArithmeticOpDto.MULTIPLY;
            case OPERATION_UNSPECIFIED, UNRECOGNIZED -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }
}
