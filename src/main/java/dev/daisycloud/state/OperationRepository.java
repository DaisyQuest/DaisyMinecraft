package dev.daisycloud.state;

import java.util.List;
import java.util.Optional;

/**
 * Repository contract for operation state.
 */
public interface OperationRepository {
    OperationRecord create(OperationRecord operation);

    Optional<OperationRecord> get(String operationId);

    List<OperationRecord> list();

    OperationRecord update(OperationRecord operation);

    Optional<OperationRecord> delete(String operationId);
}
