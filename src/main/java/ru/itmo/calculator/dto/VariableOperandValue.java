package ru.itmo.calculator.dto;

import java.util.Objects;

/**
 * Wrapper for variable references in OpenAPI requests.
 */
public final class VariableOperandValue implements Operand {
    private final String name;

    public VariableOperandValue(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VariableOperandValue that)) {
            return false;
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
