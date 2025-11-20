package ru.itmo.calculator.execution;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
    private final Executor executor;
    private final Duration operationDelay;
    private final Consumer<String> operationListener;

    public InstructionExecutionService() {
        this(ForkJoinPool.commonPool(), Duration.ofMillis(50), var -> {});
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

        Set<String> requiredVariables = collectRequiredVariables(printInstructions, calculations);
        detectCycles(requiredVariables, calculations);

        Map<String, CompletableFuture<Long>> cache = new ConcurrentHashMap<>();
        List<PrintResult> results = new ArrayList<>();

        for (PrintInstruction print : printInstructions) {
            CompletableFuture<Long> future = resolveVariable(print.var(), requiredVariables, calculations, cache);
            results.add(new PrintResult(print.var(), future.join()));
        }

        return results;
    }

    private Set<String> collectRequiredVariables(
            List<PrintInstruction> printInstructions, Map<String, CalcInstruction> calculations) {
        Set<String> required = new LinkedHashSet<>();
        for (PrintInstruction print : printInstructions) {
            collectForVariable(print.var(), required, calculations, new HashSet<>());
        }
        return required;
    }

    private void collectForVariable(
            String var,
            Set<String> required,
            Map<String, CalcInstruction> calculations,
            Set<String> visiting) {
        if (required.contains(var)) {
            return;
        }
        CalcInstruction instruction = calculations.get(var);
        if (instruction == null) {
            throw new IllegalArgumentException("Variable is never calculated: " + var);
        }
        if (!visiting.add(var)) {
            // Early cycle detection to avoid stack overflow; full message produced later.
            throw new IllegalArgumentException("Cyclic dependency detected for variable: " + var);
        }
        for (String dependency : variableDependencies(instruction)) {
            collectForVariable(dependency, required, calculations, visiting);
        }
        visiting.remove(var);
        required.add(var);
    }

    private void detectCycles(Set<String> requiredVariables, Map<String, CalcInstruction> calculations) {
        Map<String, VisitState> states = new HashMap<>();
        for (String var : requiredVariables) {
            if (states.get(var) == null) {
                dfs(var, calculations, requiredVariables, states);
            }
        }
    }

    private void dfs(
            String var,
            Map<String, CalcInstruction> calculations,
            Set<String> requiredVariables,
            Map<String, VisitState> states) {
        states.put(var, VisitState.IN_PROGRESS);
        CalcInstruction instruction = calculations.get(var);
        if (instruction == null) {
            return;
        }
        for (String dep : variableDependencies(instruction)) {
            if (!requiredVariables.contains(dep)) {
                continue;
            }
            VisitState state = states.get(dep);
            if (state == VisitState.IN_PROGRESS) {
                throw new IllegalArgumentException("Cyclic dependency detected: " + dep + " <-> " + var);
            }
            if (state == VisitState.DONE) {
                continue;
            }
            dfs(dep, calculations, requiredVariables, states);
        }
        states.put(var, VisitState.DONE);
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

    private CompletableFuture<Long> resolveOperand(
            Operand operand,
            Set<String> requiredVariables,
            Map<String, CalcInstruction> calculations,
            Map<String, CompletableFuture<Long>> cache) {
        if (operand instanceof LiteralOperand literal) {
            return CompletableFuture.completedFuture(literal.value());
        }
        if (operand instanceof VariableOperand variable) {
            return resolveVariable(variable.name(), requiredVariables, calculations, cache);
        }
        throw new IllegalArgumentException("Unknown operand: " + operand);
    }

    private CompletableFuture<Long> resolveVariable(
            String var,
            Set<String> requiredVariables,
            Map<String, CalcInstruction> calculations,
            Map<String, CompletableFuture<Long>> cache) {
        if (!requiredVariables.contains(var)) {
            throw new IllegalArgumentException("Variable is not required: " + var);
        }
        return cache.computeIfAbsent(
                var,
                name -> {
                    CalcInstruction instruction = calculations.get(name);
                    if (instruction == null) {
                        throw new IllegalArgumentException("Variable is never calculated: " + name);
                    }
                    CompletableFuture<Long> leftFuture =
                            resolveOperand(instruction.left(), requiredVariables, calculations, cache);
                    CompletableFuture<Long> rightFuture =
                            resolveOperand(instruction.right(), requiredVariables, calculations, cache);
                    return leftFuture.thenCombineAsync(
                            rightFuture,
                            (left, right) -> {
                                waitIfNeeded();
                                operationListener.accept(name);
                                return applyOperation(instruction.op(), left, right);
                            },
                            executor);
                });
    }

    private long applyOperation(ArithmeticOp op, long left, long right) {
        return switch (op) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
        };
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

    private enum VisitState {
        IN_PROGRESS,
        DONE
    }
}
