package ru.itmo.calculator.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ConcurrentHashMap;
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
    void returnsEmptyWhenNoPrintsProvided() {
        AtomicInteger executed = new AtomicInteger();
        InstructionExecutionService service =
                new InstructionExecutionService(ForkJoinPool.commonPool(), Duration.ZERO, var -> executed.incrementAndGet());

        List<Instruction> program =
                List.of(
                        new CalcInstruction("x", ArithmeticOp.ADD, new LiteralOperand(1), new LiteralOperand(2)),
                        new CalcInstruction("y", ArithmeticOp.MULTIPLY, new VariableOperand("x"), new LiteralOperand(3)));

        List<PrintResult> result = service.execute(program);

        assertEquals(List.of(), result);
        assertEquals(0, executed.get(), "No operations should run when nothing is printed");
    }

    @Test
    void cachesComputedValuesAcrossPrints() {
        List<String> executed = Collections.synchronizedList(new ArrayList<>());
        InstructionExecutionService service =
                new InstructionExecutionService(command -> command.run(), Duration.ZERO, executed::add);

        List<Instruction> program =
                List.of(
                        new CalcInstruction("base", ArithmeticOp.ADD, new LiteralOperand(2), new LiteralOperand(3)),
                        new CalcInstruction(
                                "double", ArithmeticOp.MULTIPLY, new VariableOperand("base"), new LiteralOperand(2)),
                        new CalcInstruction(
                                "minus", ArithmeticOp.SUBTRACT, new VariableOperand("base"), new LiteralOperand(1)),
                        new PrintInstruction("double"),
                        new PrintInstruction("minus"),
                        new PrintInstruction("base"),
                        new PrintInstruction("base"));

        List<PrintResult> result = service.execute(program);

        assertEquals(
                List.of(
                        new PrintResult("double", 10),
                        new PrintResult("minus", 4),
                        new PrintResult("base", 5),
                        new PrintResult("base", 5)),
                result);
        assertEquals(3, executed.size(), "Each variable should only be calculated once");
        assertTrue(executed.containsAll(List.of("base", "double", "minus")));
    }

    @Test
    void failsOnNullInstructionList() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> serviceWithNoDelay().execute(null));
        assertEquals("instructions", ex.getMessage());
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

    @Test
    void computesConsecutiveIncrementChainFromAToZ() {
        InstructionExecutionService service = serviceWithNoDelay();
        List<Instruction> program = new ArrayList<>();
        program.add(new CalcInstruction("a", ArithmeticOp.ADD, new LiteralOperand(0), new LiteralOperand(1)));

        char previous = 'a';
        for (char var = 'b'; var <= 'z'; var++) {
            program.add(
                    new CalcInstruction(
                            String.valueOf(var),
                            ArithmeticOp.ADD,
                            new VariableOperand(String.valueOf(previous)),
                            new LiteralOperand(1)));
            previous = var;
        }
        program.add(new PrintInstruction("z"));

        List<PrintResult> result = service.execute(program);

        assertEquals(List.of(new PrintResult("z", 26)), result);
    }

    @Test
    void computesWideDependencyTreeFromAToZ() {
        Set<String> executed = Collections.newSetFromMap(new ConcurrentHashMap<>());
        InstructionExecutionService service =
                new InstructionExecutionService(ForkJoinPool.commonPool(), Duration.ZERO, executed::add);

        List<Instruction> program =
                List.of(
                        new CalcInstruction("a", ArithmeticOp.ADD, new LiteralOperand(0), new LiteralOperand(2)),
                        new CalcInstruction("x", ArithmeticOp.ADD, new VariableOperand("a"), new LiteralOperand(3)),
                        new CalcInstruction("y", ArithmeticOp.ADD, new VariableOperand("a"), new LiteralOperand(4)),
                        new CalcInstruction("y2", ArithmeticOp.ADD, new VariableOperand("x"), new LiteralOperand(2)),
                        new CalcInstruction("z", ArithmeticOp.ADD, new VariableOperand("a"), new VariableOperand("y2")),
                        new CalcInstruction("z2", ArithmeticOp.ADD, new VariableOperand("y"), new VariableOperand("y2")),
                        new CalcInstruction(
                                "unused", ArithmeticOp.MULTIPLY, new VariableOperand("z2"), new LiteralOperand(10)),
                        new PrintInstruction("z"),
                        new PrintInstruction("z2"));

        List<PrintResult> result = service.execute(program);

        assertEquals(List.of(new PrintResult("z", 9), new PrintResult("z2", 13)), result);
        assertEquals(6, executed.size());
        assertTrue(executed.containsAll(List.of("a", "x", "y", "y2", "z", "z2")));
        assertFalse(executed.contains("unused"));
    }

    @Test
    void computesAlphabetSpanningTreeWithSharedBranches() {
        Set<String> executed = Collections.newSetFromMap(new ConcurrentHashMap<>());
        InstructionExecutionService service =
                new InstructionExecutionService(ForkJoinPool.commonPool(), Duration.ZERO, executed::add);

        List<Instruction> program = new ArrayList<>();
        program.add(new CalcInstruction("a", ArithmeticOp.ADD, new LiteralOperand(0), new LiteralOperand(1)));

        char previous = 'a';
        for (char var = 'b'; var <= 'x'; var++) {
            program.add(new CalcInstruction(
                    String.valueOf(var),
                    ArithmeticOp.ADD,
                    new VariableOperand(String.valueOf(previous)),
                    new LiteralOperand(1)));
            previous = var;
        }

        program.add(new CalcInstruction("y", ArithmeticOp.ADD, new VariableOperand("x"), new VariableOperand("a")));
        program.add(new CalcInstruction("z", ArithmeticOp.ADD, new VariableOperand("y"), new VariableOperand("b")));
        program.add(new CalcInstruction("y2", ArithmeticOp.ADD, new VariableOperand("x"), new VariableOperand("c")));
        program.add(new CalcInstruction("z2", ArithmeticOp.ADD, new VariableOperand("y2"), new VariableOperand("y")));
        program.add(new PrintInstruction("z"));
        program.add(new PrintInstruction("z2"));

        List<PrintResult> result = service.execute(program);

        assertEquals(List.of(new PrintResult("z", 27), new PrintResult("z2", 52)), result);
        for (char var = 'a'; var <= 'z'; var++) {
            assertTrue(executed.contains(String.valueOf(var)), "Expected execution of " + var);
        }
        assertTrue(executed.containsAll(Set.of("y2", "z2")));
        assertEquals(28, executed.size(), "All variables should be executed exactly once");
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
