package ru.itmo.calculator.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.calculator.execution.ArithmeticOp;
import ru.itmo.calculator.execution.CalcInstruction;
import ru.itmo.calculator.execution.Instruction;
import ru.itmo.calculator.execution.InstructionExecutionService;
import ru.itmo.calculator.execution.LiteralOperand;
import ru.itmo.calculator.execution.PrintInstruction;
import ru.itmo.calculator.execution.PrintResult;
import ru.itmo.calculator.execution.VariableOperand;
import ru.itmo.calculator.openapi.api.CalculatorApi;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequest;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponse;
import ru.itmo.calculator.openapi.model.LiteralOperandValue;
import ru.itmo.calculator.openapi.model.PrintedValue;
import ru.itmo.calculator.openapi.model.VariableOperandValue;

@RestController
public class CalculatorController implements CalculatorApi {

    private final InstructionExecutionService executionService;

    public CalculatorController(InstructionExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public ExecuteProgramResponse executeProgram(ExecuteProgramRequest executeProgramRequest) {
        List<Instruction> domainInstructions =
                executeProgramRequest.getInstructions().stream().map(this::toDomainInstruction).toList();
        List<PrintedValue> items =
                executionService.execute(domainInstructions).stream()
                        .map(this::toPrintedValue)
                        .toList();
        return new ExecuteProgramResponse().items(items);
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

    private ru.itmo.calculator.execution.Operand toOperand(
            ru.itmo.calculator.openapi.model.Operand rawValue) {
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
