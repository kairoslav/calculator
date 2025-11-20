package ru.itmo.calculator.converter;

import java.util.List;
import org.springframework.stereotype.Component;
import ru.itmo.calculator.dto.ArithmeticOpDto;
import ru.itmo.calculator.dto.CalcInstructionDto;
import ru.itmo.calculator.dto.InstructionDto;
import ru.itmo.calculator.dto.LiteralOperandDto;
import ru.itmo.calculator.dto.OperandDto;
import ru.itmo.calculator.dto.PrintInstructionDto;
import ru.itmo.calculator.dto.PrintResultDto;
import ru.itmo.calculator.dto.VariableOperandDto;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequest;
import ru.itmo.calculator.dto.LiteralOperandValueDto;
import ru.itmo.calculator.openapi.model.PrintedValue;
import ru.itmo.calculator.dto.VariableOperandValueDto;

@Component
public class CalculatorApiConverter {

    public List<InstructionDto> toDomainInstructions(ExecuteProgramRequest request) {
        return request.getInstructions().stream().map(this::toDomainInstruction).toList();
    }

    public List<PrintedValue> toPrintedValues(List<PrintResultDto> results) {
        return results.stream().map(this::toPrintedValue).toList();
    }

    private InstructionDto toDomainInstruction(ru.itmo.calculator.openapi.model.InstructionDto instruction) {
        if (instruction instanceof ru.itmo.calculator.openapi.model.CalcInstructionDto calc) {
            return new CalcInstructionDto(
                    calc.getVar(),
                    ArithmeticOpDto.fromSymbol(calc.getOp().getValue()),
                    toOperand(calc.getLeft()),
                    toOperand(calc.getRight()));
        }
        if (instruction instanceof ru.itmo.calculator.openapi.model.PrintInstructionDto print) {
            return new PrintInstructionDto(print.getVar());
        }
        throw new IllegalArgumentException("Unsupported instruction: " + instruction);
    }

    private OperandDto toOperand(ru.itmo.calculator.openapi.model.OperandDto rawValue) {
        if (rawValue instanceof LiteralOperandValueDto literal) {
            return new LiteralOperandDto(literal.getValue());
        }
        if (rawValue instanceof VariableOperandValueDto variable) {
            return new VariableOperandDto(variable.getName());
        }
        throw new IllegalArgumentException("Unsupported operand: " + rawValue);
    }

    private PrintedValue toPrintedValue(PrintResultDto result) {
        return new PrintedValue().var(result.var()).value(result.value());
    }
}
