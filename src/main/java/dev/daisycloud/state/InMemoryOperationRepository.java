package dev.daisycloud.state;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory {@link OperationRepository} implementation for tests and bootstrapping.
 */
public final class InMemoryOperationRepository implements OperationRepository {
    private final ConcurrentMap<String, OperationRecord> operations = new ConcurrentHashMap<>();

    @Override
    public OperationRecord create(OperationRecord operation) {
        OperationRecord value = Objects.requireNonNull(operation, "operation must not be null");
        OperationRecord existing = operations.putIfAbsent(value.operationId(), value);
        if (existing != null) {
            throw new IllegalStateException("Operation already exists: " + value.operationId());
        }
        return value;
    }

    @Override
    public Optional<OperationRecord> get(String operationId) {
        return Optional.ofNullable(operations.get(requireText(operationId, "operationId")));
    }

    @Override
    public List<OperationRecord> list() {
        return operations.values().stream()
                .sorted(Comparator.comparing(OperationRecord::operationId))
                .toList();
    }

    @Override
    public OperationRecord update(OperationRecord operation) {
        OperationRecord value = Objects.requireNonNull(operation, "operation must not be null");
        OperationRecord existing = operations.replace(value.operationId(), value);
        if (existing == null) {
            throw new IllegalStateException("Operation does not exist: " + value.operationId());
        }
        return value;
    }

    @Override
    public Optional<OperationRecord> delete(String operationId) {
        return Optional.ofNullable(operations.remove(requireText(operationId, "operationId")));
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name + " must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
