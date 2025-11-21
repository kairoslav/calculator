package ru.itmo.calculator.execution;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.LongBinaryOperator;
import org.springframework.stereotype.Service;
import ru.itmo.calculator.dto.ArithmeticOp;
import ru.itmo.calculator.dto.CalcInstruction;
import ru.itmo.calculator.dto.Instruction;
import ru.itmo.calculator.dto.LiteralOperand;
import ru.itmo.calculator.dto.Operand;
import ru.itmo.calculator.dto.PrintInstruction;
import ru.itmo.calculator.dto.PrintResult;
import ru.itmo.calculator.dto.VariableOperand;

/**
 * Executes calculator instructions with dependency resolution and parallelism.
 */
@Service
public class InstructionExecutionService {
    private static final Map<ArithmeticOp, LongBinaryOperator> OPERATION_HANDLERS = Map.of(
            ArithmeticOp.ADD, (left, right) -> left + right,
            ArithmeticOp.SUBTRACT, (left, right) -> left - right,
            ArithmeticOp.MULTIPLY, (left, right) -> left * right);
    private static final int MIN_PARALLELISM = 2;

    private final Executor executor;
    private final Duration operationDelay;
    private final Consumer<String> operationListener;

    public InstructionExecutionService() {
        this(defaultExecutor(), Duration.ofMillis(1000), var -> {});
    }

    public InstructionExecutionService(Executor executor, Duration operationDelay, Consumer<String> operationListener) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.operationDelay = Objects.requireNonNull(operationDelay, "operationDelay");
        this.operationListener = operationListener == null ? var -> {} : operationListener;
    }

    public List<PrintResult> execute(List<Instruction> instructions) {
        Objects.requireNonNull(instructions, "instructions");
        Map<String, CalcInstruction> calculations = new HashMap<>();
        List<PrintInstruction> printInstructions = new ArrayList<>();

        for (Instruction instruction : instructions) {
            if (instruction instanceof CalcInstruction calc) {
                if (calculations.containsKey(calc.var())) {
                    throw new IllegalArgumentException("Variable is already defined: " + calc.var());
                }
                calculations.put(calc.var(), calc);
            } else if (instruction instanceof PrintInstruction print) {
                printInstructions.add(print);
            } else {
                throw new IllegalArgumentException("Unsupported instruction: " + instruction);
            }
        }

        if (printInstructions.isEmpty()) {
            return List.of();
        }

        ExecutionPlan executionPlan = buildExecutionPlan(printInstructions, calculations);
        Map<String, CompletableFuture<Long>> futuresByVar = startCalculations(executionPlan);

        List<PrintResult> results = new ArrayList<>();
        for (PrintInstruction print : printInstructions) {
            CompletableFuture<Long> future = futuresByVar.get(print.var());
            if (future == null) {
                throw new IllegalArgumentException("Variable is not required: " + print.var());
            }
            results.add(new PrintResult(print.var(), future.join()));
        }

        return results;
    }

    private ExecutionPlan buildExecutionPlan(
            List<PrintInstruction> printInstructions, Map<String, CalcInstruction> calculations) {
        Set<String> required = new LinkedHashSet<>();
        Map<String, List<String>> dependenciesByVar = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();

        for (PrintInstruction print : printInstructions) {
            stack.push(print.var());
        }

        while (!stack.isEmpty()) {
            String var = stack.pop();
            if (!required.add(var)) {
                continue;
            }
            CalcInstruction instruction = calculations.get(var);
            if (instruction == null) {
                throw new IllegalArgumentException("Variable is never calculated: " + var);
            }
            List<String> deps = variableDependencies(instruction);
            dependenciesByVar.put(var, deps);
            for (String dep : deps) {
                stack.push(dep);
            }
        }

        List<String> executionOrder = topologicallySort(required, dependenciesByVar);
        return new ExecutionPlan(required, calculations, executionOrder);
    }

    private Map<String, CompletableFuture<Long>> startCalculations(ExecutionPlan plan) {
        Map<String, CompletableFuture<Long>> futuresByVar = new HashMap<>(plan.requiredVariables().size());

        for (String var : plan.executionOrder()) {
            CalcInstruction instruction = plan.calculations().get(var);
            CompletableFuture<Long> leftFuture = resolveOperand(instruction.left(), futuresByVar);
            CompletableFuture<Long> rightFuture = resolveOperand(instruction.right(), futuresByVar);

            CompletableFuture<Long> result =
                    leftFuture.thenCombineAsync(rightFuture, (left, right) -> computeOperation(instruction, left, right), executor);
            futuresByVar.put(var, result);
        }

        return futuresByVar;
    }

    private CompletableFuture<Long> resolveOperand(Operand operand, Map<String, CompletableFuture<Long>> futuresByVar) {
        if (operand instanceof LiteralOperand literal) {
            return CompletableFuture.completedFuture(literal.value());
        }
        if (operand instanceof VariableOperand variable) {
            CompletableFuture<Long> future = futuresByVar.get(variable.name());
            if (future == null) {
                throw new IllegalArgumentException("Variable is not required: " + variable.name());
            }
            return future;
        }
        throw new IllegalArgumentException("Unknown operand: " + operand);
    }

    private List<String> topologicallySort(Set<String> required, Map<String, List<String>> dependenciesByVar) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();

        for (String var : required) {
            indegree.put(var, 0);
        }

        for (Map.Entry<String, List<String>> entry : dependenciesByVar.entrySet()) {
            String var = entry.getKey();
            for (String dependency : entry.getValue()) {
                indegree.merge(var, 1, Integer::sum);
                dependents.computeIfAbsent(dependency, key -> new ArrayList<>()).add(var);
            }
        }

        Deque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>(required.size());
        while (!queue.isEmpty()) {
            String var = queue.poll();
            order.add(var);
            for (String dependent : dependents.getOrDefault(var, List.of())) {
                int remaining = indegree.computeIfPresent(dependent, (key, value) -> value - 1);
                if (remaining == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (order.size() != required.size()) {
            throw new IllegalArgumentException("Cyclic dependency detected in required variables");
        }

        return order;
    }

    private List<String> variableDependencies(CalcInstruction instruction) {
        List<String> deps = new ArrayList<>();
        if (instruction.left() instanceof VariableOperand(String name)) {
            deps.add(name);
        }
        if (instruction.right() instanceof VariableOperand(String name)) {
            deps.add(name);
        }
        return deps;
    }

    private long computeOperation(CalcInstruction instruction, long left, long right) {
        Long fastResult = tryShortCircuit(instruction.op(), left, right);
        if (fastResult != null) {
            operationListener.accept(instruction.var());
            return fastResult;
        }

        waitIfNeeded();
        operationListener.accept(instruction.var());
        return OPERATION_HANDLERS.get(instruction.op()).applyAsLong(left, right);
    }

    private Long tryShortCircuit(ArithmeticOp op, long left, long right) {
        if (op == ArithmeticOp.MULTIPLY) {
            if (left == 0 || right == 0) {
                return 0L;
            }
            if (left == 1) {
                return right;
            }
            if (right == 1) {
                return left;
            }
        }
        if (op == ArithmeticOp.ADD) {
            if (left == 0) {
                return right;
            }
            if (right == 0) {
                return left;
            }
        }
        if (op == ArithmeticOp.SUBTRACT && right == 0) {
            return left;
        }
        return null;
    }

    private void waitIfNeeded() {
        if (operationDelay.isZero() || operationDelay.isNegative()) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(operationDelay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing operation", e);
        }
    }

    private static ExecutorService defaultExecutor() {
        int parallelism = Math.max(MIN_PARALLELISM, Runtime.getRuntime().availableProcessors());
        AtomicInteger counter = new AtomicInteger();
        return Executors.newFixedThreadPool(parallelism, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("calculator-exec-" + counter.incrementAndGet());
            return thread;
        });
    }

    private record ExecutionPlan(
            Set<String> requiredVariables, Map<String, CalcInstruction> calculations, List<String> executionOrder) {
    }
}
