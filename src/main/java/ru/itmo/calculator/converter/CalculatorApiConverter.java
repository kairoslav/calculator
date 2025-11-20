package ru.itmo.calculator.converter;

import java.util.List;
import org.springframework.stereotype.Component;
import ru.itmo.calculator.dto.ArithmeticOp;
import ru.itmo.calculator.dto.CalcInstruction;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.dto.LiteralOperand;
import ru.itmo.calculator.dto.Operand;
import ru.itmo.calculator.dto.PrintInstruction;
import ru.itmo.calculator.dto.PrintResult;
import ru.itmo.calculator.dto.VariableOperand;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequestDto;
import ru.itmo.calculator.dto.LiteralOperandValue;
import ru.itmo.calculator.openapi.model.PrintedValueDto;
import ru.itmo.calculator.dto.VariableOperandValue;

@Component
public class CalculatorApiConverter {

    public List<Instruction> toDomainInstructions(ExecuteProgramRequestDto request) {
        return request.getInstructions().stream().map(this::toDomainInstruction).toList();
    }

    public List<PrintedValueDto> toPrintedValues(List<PrintResult> results) {
        return results.stream().map(this::toPrintedValue).toList();
    }

    private Instruction toDomainInstruction(ru.itmo.calculator.openapi.model.InstructionDto instruction) {
        if (instruction instanceof ru.itmo.calculator.openapi.model.CalcInstructionDto calc) {
            return new CalcInstruction(
                    calc.getVar(),
                    ArithmeticOp.fromSymbol(calc.getOp().getValue()),
                    toOperand(calc.getLeft()),
                    toOperand(calc.getRight()));
        }
        if (instruction instanceof ru.itmo.calculator.openapi.model.PrintInstructionDto print) {
            return new PrintInstruction(print.getVar());
        }
        throw new IllegalArgumentException("Unsupported instruction: " + instruction);
    }

    private Operand toOperand(ru.itmo.calculator.openapi.model.OperandDto rawValue) {
        if (rawValue instanceof LiteralOperandValue literal) {
            return new LiteralOperand(literal.getValue());
        }
        if (rawValue instanceof VariableOperandValue variable) {
            return new VariableOperand(variable.getName());
        }
        throw new IllegalArgumentException("Unsupported operand: " + rawValue);
    }

    private PrintedValueDto toPrintedValue(PrintResult result) {
        return new PrintedValueDto().var(result.var()).value(result.value());
    }
}
