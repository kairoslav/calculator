package ru.itmo.calculator.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.itmo.calculator.dto.ArithmeticOp;
import ru.itmo.calculator.dto.CalcInstruction;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.dto.LiteralOperand;
import ru.itmo.calculator.dto.PrintInstruction;
import ru.itmo.calculator.dto.PrintResult;
import ru.itmo.calculator.dto.VariableOperand;

class InstructionExecutionServiceTest {

    private InstructionExecutionService serviceWithNoDelay() {
        return new InstructionExecutionService(ForkJoinPool.commonPool(), Duration.ZERO, null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("taskExamples")
    void executesTaskExamples(String name, List<Instruction> program, List<PrintResult> expected) {
        List<PrintResult> result = serviceWithNoDelay().execute(program);

        assertEquals(expected, result, name);
    }

    @Test
    void computesLinearDependencies() {
        List<Instruction> program =
                List.of(
                        new CalcInstruction("x", ArithmeticOp.ADD, new LiteralOperand(1), new LiteralOperand(2)),
                        new PrintInstruction("x"));

        List<PrintResult> result = serviceWithNoDelay().execute(program);

        assertEquals(List.of(new PrintResult("x", 3)), result);
    }

    @Test
    void skipsUnusedCalculations() {
        AtomicInteger executed = new AtomicInteger();
        InstructionExecutionService service =
                new InstructionExecutionService(ForkJoinPool.commonPool(), Duration.ZERO, var -> executed.incrementAndGet());

        List<Instruction> program =
                List.of(
                        new CalcInstruction("x", ArithmeticOp.ADD, new LiteralOperand(10), new LiteralOperand(2)),
                        new CalcInstruction("y", ArithmeticOp.MULTIPLY, new VariableOperand("x"), new LiteralOperand(5)),
                        new CalcInstruction("q", ArithmeticOp.SUBTRACT, new VariableOperand("y"), new LiteralOperand(20)),
                        new CalcInstruction("unusedA", ArithmeticOp.ADD, new VariableOperand("y"), new LiteralOperand(100)),
                        new CalcInstruction(
                                "unusedB", ArithmeticOp.MULTIPLY, new VariableOperand("unusedA"), new LiteralOperand(2)),
                        new PrintInstruction("q"),
                        new CalcInstruction("z", ArithmeticOp.SUBTRACT, new VariableOperand("x"), new LiteralOperand(15)),
                        new PrintInstruction("z"),
                        new CalcInstruction("ignoreC", ArithmeticOp.ADD, new VariableOperand("z"), new VariableOperand("y")),
                        new PrintInstruction("x"));

        List<PrintResult> result = service.execute(program);

        assertEquals(List.of(new PrintResult("q", 40), new PrintResult("z", -3), new PrintResult("x", 12)), result);
        assertEquals(4, executed.get(), "Only required operations should be executed");
    }

    @Test
    void detectsCycles() {
        List<Instruction> program =
                List.of(
                        new CalcInstruction("a", ArithmeticOp.ADD, new VariableOperand("b"), new LiteralOperand(1)),
                        new CalcInstruction("b", ArithmeticOp.ADD, new VariableOperand("a"), new LiteralOperand(1)),
                        new PrintInstruction("a"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> serviceWithNoDelay().execute(program));
        assertContains(ex.getMessage(), "Cyclic");
    }

    @Test
    void rejectsDuplicateDefinitions() {
        List<Instruction> program =
                List.of(
                        new CalcInstruction("x", ArithmeticOp.ADD, new LiteralOperand(1), new LiteralOperand(1)),
                        new CalcInstruction("x", ArithmeticOp.SUBTRACT, new LiteralOperand(2), new LiteralOperand(1)),
                        new PrintInstruction("x"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> serviceWithNoDelay().execute(program));
        assertContains(ex.getMessage(), "already defined");
    }

    @Test
    void failsWhenPrintsMissingVariable() {
        List<Instruction> program = List.of(new PrintInstruction("missing"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> serviceWithNoDelay().execute(program));
        assertContains(ex.getMessage(), "never calculated");
    }

    @Test
    void failsWhenDependencyMissing() {
        List<Instruction> program =
                List.of(
                        new CalcInstruction("x", ArithmeticOp.ADD, new VariableOperand("y"), new LiteralOperand(1)),
                        new PrintInstruction("x"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> serviceWithNoDelay().execute(program));
        assertContains(ex.getMessage(), "never calculated");
    }

    private static void assertContains(String actual, String expected) {
        if (actual == null || !actual.contains(expected)) {
            throw new AssertionError("Expected message to contain '" + expected + "' but was '" + actual + "'");
        }
    }

    private static Stream<Arguments> taskExamples() {
        return Stream.of(
                Arguments.of(
                        "Task example 1: simple add and print",
                        List.of(
                                new CalcInstruction(
                                        "x", ArithmeticOp.ADD, new LiteralOperand(1), new LiteralOperand(2)),
                                new PrintInstruction("x")),
                        List.of(new PrintResult("x", 3))),
                Arguments.of(
                        "Task example 2: out-of-order print and zero multiplication",
                        List.of(
                                new CalcInstruction(
                                        "x", ArithmeticOp.ADD, new LiteralOperand(10), new LiteralOperand(2)),
                                new PrintInstruction("x"),
                                new CalcInstruction(
                                        "y", ArithmeticOp.SUBTRACT, new VariableOperand("x"), new LiteralOperand(3)),
                                new CalcInstruction(
                                        "z", ArithmeticOp.MULTIPLY, new VariableOperand("x"), new VariableOperand("y")),
                                new PrintInstruction("w"),
                                new CalcInstruction(
                                        "w", ArithmeticOp.MULTIPLY, new VariableOperand("z"), new LiteralOperand(0))),
                        List.of(new PrintResult("x", 12), new PrintResult("w", 0))),
                Arguments.of(
                        "Task example 3: skip unused branches and preserve print order",
                        List.of(
                                new CalcInstruction(
                                        "x", ArithmeticOp.ADD, new LiteralOperand(10), new LiteralOperand(2)),
                                new CalcInstruction(
                                        "y", ArithmeticOp.MULTIPLY, new VariableOperand("x"), new LiteralOperand(5)),
                                new CalcInstruction(
                                        "q", ArithmeticOp.SUBTRACT, new VariableOperand("y"), new LiteralOperand(20)),
                                new CalcInstruction(
                                        "unusedA", ArithmeticOp.ADD, new VariableOperand("y"), new LiteralOperand(100)),
                                new CalcInstruction(
                                        "unusedB",
                                        ArithmeticOp.MULTIPLY,
                                        new VariableOperand("unusedA"),
                                        new LiteralOperand(2)),
                                new PrintInstruction("q"),
                                new CalcInstruction(
                                        "z", ArithmeticOp.SUBTRACT, new VariableOperand("x"), new LiteralOperand(15)),
                                new PrintInstruction("z"),
                                new CalcInstruction(
                                        "ignoreC", ArithmeticOp.ADD, new VariableOperand("z"), new VariableOperand("y")),
                                new PrintInstruction("x")),
                        List.of(
                                new PrintResult("q", 40),
                                new PrintResult("z", -3),
                                new PrintResult("x", 12))));
    }
}
