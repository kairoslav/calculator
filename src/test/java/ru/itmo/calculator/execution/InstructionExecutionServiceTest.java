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
import ru.itmo.calculator.dto.ArithmeticOpDto;
import ru.itmo.calculator.dto.CalcInstructionDto;
import ru.itmo.calculator.dto.InstructionDto;
import ru.itmo.calculator.dto.LiteralOperandDto;
import ru.itmo.calculator.dto.PrintInstructionDto;
import ru.itmo.calculator.dto.PrintResultDto;
import ru.itmo.calculator.dto.VariableOperandDto;

class InstructionExecutionServiceTest {

    private InstructionExecutionService serviceWithNoDelay() {
        return new InstructionExecutionService(ForkJoinPool.commonPool(), Duration.ZERO, null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("taskExamples")
    void executesTaskExamples(String name, List<InstructionDto> program, List<PrintResultDto> expected) {
        List<PrintResultDto> result = serviceWithNoDelay().execute(program);

        assertEquals(expected, result, name);
    }

    @Test
    void computesLinearDependencies() {
        List<InstructionDto> program =
                List.of(
                        new CalcInstructionDto("x", ArithmeticOpDto.ADD, new LiteralOperandDto(1), new LiteralOperandDto(2)),
                        new PrintInstructionDto("x"));

        List<PrintResultDto> result = serviceWithNoDelay().execute(program);

        assertEquals(List.of(new PrintResultDto("x", 3)), result);
    }

    @Test
    void skipsUnusedCalculations() {
        AtomicInteger executed = new AtomicInteger();
        InstructionExecutionService service =
                new InstructionExecutionService(ForkJoinPool.commonPool(), Duration.ZERO, var -> executed.incrementAndGet());

        List<InstructionDto> program =
                List.of(
                        new CalcInstructionDto("x", ArithmeticOpDto.ADD, new LiteralOperandDto(10), new LiteralOperandDto(2)),
                        new CalcInstructionDto("y", ArithmeticOpDto.MULTIPLY, new VariableOperandDto("x"), new LiteralOperandDto(5)),
                        new CalcInstructionDto("q", ArithmeticOpDto.SUBTRACT, new VariableOperandDto("y"), new LiteralOperandDto(20)),
                        new CalcInstructionDto("unusedA", ArithmeticOpDto.ADD, new VariableOperandDto("y"), new LiteralOperandDto(100)),
                        new CalcInstructionDto(
                                "unusedB", ArithmeticOpDto.MULTIPLY, new VariableOperandDto("unusedA"), new LiteralOperandDto(2)),
                        new PrintInstructionDto("q"),
                        new CalcInstructionDto("z", ArithmeticOpDto.SUBTRACT, new VariableOperandDto("x"), new LiteralOperandDto(15)),
                        new PrintInstructionDto("z"),
                        new CalcInstructionDto("ignoreC", ArithmeticOpDto.ADD, new VariableOperandDto("z"), new VariableOperandDto("y")),
                        new PrintInstructionDto("x"));

        List<PrintResultDto> result = service.execute(program);

        assertEquals(List.of(new PrintResultDto("q", 40), new PrintResultDto("z", -3), new PrintResultDto("x", 12)), result);
        assertEquals(4, executed.get(), "Only required operations should be executed");
    }

    @Test
    void detectsCycles() {
        List<InstructionDto> program =
                List.of(
                        new CalcInstructionDto("a", ArithmeticOpDto.ADD, new VariableOperandDto("b"), new LiteralOperandDto(1)),
                        new CalcInstructionDto("b", ArithmeticOpDto.ADD, new VariableOperandDto("a"), new LiteralOperandDto(1)),
                        new PrintInstructionDto("a"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> serviceWithNoDelay().execute(program));
        assertContains(ex.getMessage(), "Cyclic");
    }

    @Test
    void rejectsDuplicateDefinitions() {
        List<InstructionDto> program =
                List.of(
                        new CalcInstructionDto("x", ArithmeticOpDto.ADD, new LiteralOperandDto(1), new LiteralOperandDto(1)),
                        new CalcInstructionDto("x", ArithmeticOpDto.SUBTRACT, new LiteralOperandDto(2), new LiteralOperandDto(1)),
                        new PrintInstructionDto("x"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> serviceWithNoDelay().execute(program));
        assertContains(ex.getMessage(), "already defined");
    }

    @Test
    void failsWhenPrintsMissingVariable() {
        List<InstructionDto> program = List.of(new PrintInstructionDto("missing"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> serviceWithNoDelay().execute(program));
        assertContains(ex.getMessage(), "never calculated");
    }

    @Test
    void failsWhenDependencyMissing() {
        List<InstructionDto> program =
                List.of(
                        new CalcInstructionDto("x", ArithmeticOpDto.ADD, new VariableOperandDto("y"), new LiteralOperandDto(1)),
                        new PrintInstructionDto("x"));

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
                                new CalcInstructionDto(
                                        "x", ArithmeticOpDto.ADD, new LiteralOperandDto(1), new LiteralOperandDto(2)),
                                new PrintInstructionDto("x")),
                        List.of(new PrintResultDto("x", 3))),
                Arguments.of(
                        "Task example 2: out-of-order print and zero multiplication",
                        List.of(
                                new CalcInstructionDto(
                                        "x", ArithmeticOpDto.ADD, new LiteralOperandDto(10), new LiteralOperandDto(2)),
                                new PrintInstructionDto("x"),
                                new CalcInstructionDto(
                                        "y", ArithmeticOpDto.SUBTRACT, new VariableOperandDto("x"), new LiteralOperandDto(3)),
                                new CalcInstructionDto(
                                        "z", ArithmeticOpDto.MULTIPLY, new VariableOperandDto("x"), new VariableOperandDto("y")),
                                new PrintInstructionDto("w"),
                                new CalcInstructionDto(
                                        "w", ArithmeticOpDto.MULTIPLY, new VariableOperandDto("z"), new LiteralOperandDto(0))),
                        List.of(new PrintResultDto("x", 12), new PrintResultDto("w", 0))),
                Arguments.of(
                        "Task example 3: skip unused branches and preserve print order",
                        List.of(
                                new CalcInstructionDto(
                                        "x", ArithmeticOpDto.ADD, new LiteralOperandDto(10), new LiteralOperandDto(2)),
                                new CalcInstructionDto(
                                        "y", ArithmeticOpDto.MULTIPLY, new VariableOperandDto("x"), new LiteralOperandDto(5)),
                                new CalcInstructionDto(
                                        "q", ArithmeticOpDto.SUBTRACT, new VariableOperandDto("y"), new LiteralOperandDto(20)),
                                new CalcInstructionDto(
                                        "unusedA", ArithmeticOpDto.ADD, new VariableOperandDto("y"), new LiteralOperandDto(100)),
                                new CalcInstructionDto(
                                        "unusedB",
                                        ArithmeticOpDto.MULTIPLY,
                                        new VariableOperandDto("unusedA"),
                                        new LiteralOperandDto(2)),
                                new PrintInstructionDto("q"),
                                new CalcInstructionDto(
                                        "z", ArithmeticOpDto.SUBTRACT, new VariableOperandDto("x"), new LiteralOperandDto(15)),
                                new PrintInstructionDto("z"),
                                new CalcInstructionDto(
                                        "ignoreC", ArithmeticOpDto.ADD, new VariableOperandDto("z"), new VariableOperandDto("y")),
                                new PrintInstructionDto("x")),
                        List.of(
                                new PrintResultDto("q", 40),
                                new PrintResultDto("z", -3),
                                new PrintResultDto("x", 12))));
    }
}
