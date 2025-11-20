package ru.itmo.calculator.grpc;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import ru.itmo.calculator.dto.ArithmeticOp;
import ru.itmo.calculator.dto.CalcInstruction;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.dto.LiteralOperand;
import ru.itmo.calculator.dto.Operand;
import ru.itmo.calculator.dto.PrintInstruction;
import ru.itmo.calculator.dto.PrintResult;
import ru.itmo.calculator.dto.VariableOperand;

@Component
public class GrpcInstructionConverter {

    public List<Instruction> toDomainInstructions(ExecuteProgramRequest request) {
        Objects.requireNonNull(request, "request");
        return request.getInstructionsList().stream().map(this::toDomainInstruction).toList();
    }

    public ExecuteProgramResponse toResponse(List<PrintResult> results) {
        ExecuteProgramResponse.Builder builder = ExecuteProgramResponse.newBuilder();

        for (PrintResult result : results) {
            builder.addItems(
                    PrintedValue.newBuilder()
                            .setVar(result.var())
                            .setValue(result.value())
                            .build());
        }

        return builder.build();
    }

    private Instruction toDomainInstruction(ru.itmo.calculator.grpc.Instruction instruction) {
        return switch (instruction.getInstructionKindCase()) {
            case CALC -> toCalcInstruction(instruction.getCalc());
            case PRINT -> new PrintInstruction(instruction.getPrint().getVar());
            case INSTRUCTIONKIND_NOT_SET -> throw new IllegalArgumentException("Instruction kind is required");
        };
    }

    private CalcInstruction toCalcInstruction(ru.itmo.calculator.grpc.CalcInstruction calc) {
        return new CalcInstruction(
                calc.getVar(), toArithmeticOp(calc.getOp()), toOperand(calc.getLeft()), toOperand(calc.getRight()));
    }

    private Operand toOperand(ru.itmo.calculator.grpc.Operand operand) {
        return switch (operand.getValueCase()) {
            case LITERAL -> new LiteralOperand(operand.getLiteral());
            case VARIABLE -> new VariableOperand(operand.getVariable());
            case VALUE_NOT_SET -> throw new IllegalArgumentException("Operand value is required");
        };
    }

    private ArithmeticOp toArithmeticOp(Operation operation) {
        return switch (operation) {
            case OPERATION_ADD -> ArithmeticOp.ADD;
            case OPERATION_SUBTRACT -> ArithmeticOp.SUBTRACT;
            case OPERATION_MULTIPLY -> ArithmeticOp.MULTIPLY;
            case OPERATION_UNSPECIFIED, UNRECOGNIZED -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }
}
