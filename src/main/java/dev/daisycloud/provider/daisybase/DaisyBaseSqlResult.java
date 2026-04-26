package dev.daisycloud.provider.daisybase;

import java.util.List;
import java.util.Objects;

public record DaisyBaseSqlResult(
        boolean success,
        String message,
        List<String> columns,
        List<List<String>> rows,
        int updateCount) {

    public DaisyBaseSqlResult {
        message = Text.require(message, "message");
        columns = List.copyOf(Objects.requireNonNull(columns, "columns must not be null"));
        rows = rows.stream()
                .map(row -> List.copyOf(Objects.requireNonNull(row, "rows must not contain null")))
                .toList();
        if (!success && (!columns.isEmpty() || !rows.isEmpty() || updateCount != 0)) {
            throw new IllegalArgumentException("failed results must not include columns, rows, or update counts");
        }
        if (updateCount < 0) {
            throw new IllegalArgumentException("updateCount must not be negative");
        }
    }

    public static DaisyBaseSqlResult update(String message, int updateCount) {
        return new DaisyBaseSqlResult(true, message, List.of(), List.of(), updateCount);
    }

    public static DaisyBaseSqlResult query(List<String> columns, List<List<String>> rows) {
        return new DaisyBaseSqlResult(true, "selected " + rows.size() + " row(s)", columns, rows, rows.size());
    }

    public static DaisyBaseSqlResult failed(String message) {
        return new DaisyBaseSqlResult(false, message, List.of(), List.of(), 0);
    }
}
