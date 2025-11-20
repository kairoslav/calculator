package ru.itmo.calculator.dto;

import java.util.Objects;

/**
 * Wrapper for literal values in OpenAPI requests.
 */
public final class LiteralOperandValueDto implements OperandDto {
    private final long value;

    public LiteralOperandValueDto(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteralOperandValueDto that)) {
            return false;
        }
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
