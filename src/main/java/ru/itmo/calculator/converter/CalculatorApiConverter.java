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
import ru.itmo.calculator.openapi.model.ExecuteProgramRequest;
import ru.itmo.calculator.openapi.model.LiteralOperandValue;
import ru.itmo.calculator.openapi.model.PrintedValue;
import ru.itmo.calculator.openapi.model.VariableOperandValue;

@Component
public class CalculatorApiConverter {

    public List<Instruction> toDomainInstructions(ExecuteProgramRequest request) {
        return request.getInstructions().stream().map(this::toDomainInstruction).toList();
    }

    public List<PrintedValue> toPrintedValues(List<PrintResult> results) {
        return results.stream().map(this::toPrintedValue).toList();
    }

    private Instruction toDomainInstruction(ru.itmo.calculator.openapi.model.Instruction instruction) {
        if (instruction instanceof ru.itmo.calculator.openapi.model.CalcInstruction calc) {
            return new CalcInstruction(
                    calc.getVar(),
                    ArithmeticOp.fromSymbol(calc.getOp().getValue()),
                    toOperand(calc.getLeft()),
                    toOperand(calc.getRight()));
        }
        if (instruction instanceof ru.itmo.calculator.openapi.model.PrintInstruction print) {
            return new PrintInstruction(print.getVar());
        }
        throw new IllegalArgumentException("Unsupported instruction: " + instruction);
    }

    private Operand toOperand(ru.itmo.calculator.openapi.model.Operand rawValue) {
        if (rawValue instanceof LiteralOperandValue literal) {
            return new LiteralOperand(literal.getValue());
        }
        if (rawValue instanceof VariableOperandValue variable) {
            return new VariableOperand(variable.getName());
        }
        throw new IllegalArgumentException("Unsupported operand: " + rawValue);
    }

    private PrintedValue toPrintedValue(PrintResult result) {
        return new PrintedValue().var(result.var()).value(result.value());
    }
}
