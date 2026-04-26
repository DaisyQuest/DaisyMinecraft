package dev.daisycloud.provider.daisybase;

import dev.daisycloud.state.ResourceRecord;
import dev.daisycloud.state.ResourceRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DaisyBaseConnector {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+([A-Za-z][A-Za-z0-9_]*)\\s*\\((.+)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_TABLE = Pattern.compile(
            "DROP\\s+TABLE\\s+([A-Za-z][A-Za-z0-9_]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRUNCATE_TABLE = Pattern.compile(
            "TRUNCATE\\s+TABLE\\s+([A-Za-z][A-Za-z0-9_]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_TABLE_ADD_COLUMN = Pattern.compile(
            "ALTER\\s+TABLE\\s+([A-Za-z][A-Za-z0-9_]*)\\s+ADD\\s+COLUMN\\s+([A-Za-z][A-Za-z0-9_]*)\\s+(INT|TEXT)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT = Pattern.compile(
            "INSERT\\s+INTO\\s+([A-Za-z][A-Za-z0-9_]*)\\s*\\((.+)\\)\\s+VALUES\\s*\\((.+)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SELECT = Pattern.compile(
            "SELECT\\s+(.+)\\s+FROM\\s+([A-Za-z][A-Za-z0-9_]*)(?:\\s+WHERE\\s+([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*(.+?))?(?:\\s+ORDER\\s+BY\\s+([A-Za-z][A-Za-z0-9_]*))?(?:\\s+LIMIT\\s+([0-9]+))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOW_TABLES = Pattern.compile(
            "SHOW\\s+TABLES",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIBE_TABLE = Pattern.compile(
            "(?:DESCRIBE|DESC)\\s+([A-Za-z][A-Za-z0-9_]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE = Pattern.compile(
            "UPDATE\\s+([A-Za-z][A-Za-z0-9_]*)\\s+SET\\s+(.+)\\s+WHERE\\s+([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*(.+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE = Pattern.compile(
            "DELETE\\s+FROM\\s+([A-Za-z][A-Za-z0-9_]*)\\s+WHERE\\s+([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*(.+)",
            Pattern.CASE_INSENSITIVE);
    private static final String SCHEMA_PREFIX = "daisybase.table.";
    private static final String SCHEMA_SUFFIX = ".schema";
    private static final String ROWS_SUFFIX = ".rows";

    private final ResourceRepository resources;

    public DaisyBaseConnector(ResourceRepository resources) {
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
    }

    public DaisyBaseConnectionResult connect(String databaseResourceId) {
        String id = Text.require(databaseResourceId, "databaseResourceId");
        ResourceRecord database = resources.get(id).orElse(null);
        if (database == null) {
            return DaisyBaseConnectionResult.failed("Database resource not found: " + id);
        }
        if (!DaisyBaseProviderCatalog.PROVIDER_ID.equals(database.providerId())
                || !DaisyBaseProviderCatalog.DATABASE_RESOURCE_TYPE.equals(database.resourceTypeId())) {
            return DaisyBaseConnectionResult.failed("Resource is not a DaisyBase database: " + id);
        }

        Map<String, String> attributes = database.attributes();
        String endpoint = attributes.get("endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            return DaisyBaseConnectionResult.failed("Database endpoint is missing");
        }
        String databaseName = attributes.get("databaseName");
        if (databaseName == null || databaseName.isBlank()) {
            return DaisyBaseConnectionResult.failed("Database name is missing");
        }
        boolean writeEnabled = !"false".equalsIgnoreCase(attributes.getOrDefault("writeEnabled", "true"));
        return DaisyBaseConnectionResult.connected(new DaisyBaseConnectionDetails(
                id,
                endpoint,
                databaseName,
                writeEnabled));
    }

    public List<TableDescription> tables(String databaseResourceId) {
        ResourceRecord database = requireDatabase(databaseResourceId);
        Map<String, String> attributes = database.attributes();
        return attributes.keySet().stream()
                .filter(key -> key.startsWith(SCHEMA_PREFIX))
                .filter(key -> key.endsWith(SCHEMA_SUFFIX))
                .map(key -> key.substring(SCHEMA_PREFIX.length(), key.length() - SCHEMA_SUFFIX.length()))
                .sorted()
                .map(table -> describe(attributes, table))
                .toList();
    }

    public DaisyBaseSqlResult importTable(
            String databaseResourceId,
            String tableName,
            String format,
            String payload,
            boolean replaceExisting) {
        ResourceRecord database = requireDatabase(databaseResourceId);
        DaisyBaseConnectionResult connection = connect(databaseResourceId);
        if (!connection.connected()) {
            return DaisyBaseSqlResult.failed(connection.message());
        }
        if (!connection.details().writeEnabled()) {
            return DaisyBaseSqlResult.failed("Database is read-only");
        }
        try {
            String table = normalizeIdentifier(tableName, "tableName");
            TabularImportData data = parseImportData(format, payload);
            Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
            if (attributes.containsKey(schemaKey(table)) && !replaceExisting) {
                return DaisyBaseSqlResult.failed("Table already exists: " + table);
            }
            TableSchema schema = new TableSchema(data.columns(), data.types());
            attributes.put(schemaKey(table), schema.encode());
            attributes.put(rowsKey(table), encodeRows(data.rows()));
            resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
            return DaisyBaseSqlResult.update(
                    "imported " + data.rows().size() + " row(s) into " + table + " from " + data.format(),
                    data.rows().size());
        } catch (IllegalArgumentException error) {
            return DaisyBaseSqlResult.failed(error.getMessage());
        }
    }

    public DaisyBaseSqlResult execute(String databaseResourceId, String sql) {
        String statement = normalizeStatement(sql);
        ResourceRecord database = resources.get(Text.require(databaseResourceId, "databaseResourceId")).orElse(null);
        if (database == null) {
            return DaisyBaseSqlResult.failed("Database resource not found: " + databaseResourceId);
        }
        DaisyBaseConnectionResult connection = connect(databaseResourceId);
        if (!connection.connected()) {
            return DaisyBaseSqlResult.failed(connection.message());
        }
        try {
            Matcher create = CREATE_TABLE.matcher(statement);
            if (create.matches()) {
                if (!connection.details().writeEnabled()) {
                    return DaisyBaseSqlResult.failed("Database is read-only");
                }
                return createTable(database, create.group(1), create.group(2));
            }
            Matcher drop = DROP_TABLE.matcher(statement);
            if (drop.matches()) {
                if (!connection.details().writeEnabled()) {
                    return DaisyBaseSqlResult.failed("Database is read-only");
                }
                return dropTable(database, drop.group(1));
            }
            Matcher truncate = TRUNCATE_TABLE.matcher(statement);
            if (truncate.matches()) {
                if (!connection.details().writeEnabled()) {
                    return DaisyBaseSqlResult.failed("Database is read-only");
                }
                return truncateTable(database, truncate.group(1));
            }
            Matcher alterAdd = ALTER_TABLE_ADD_COLUMN.matcher(statement);
            if (alterAdd.matches()) {
                if (!connection.details().writeEnabled()) {
                    return DaisyBaseSqlResult.failed("Database is read-only");
                }
                return alterTableAddColumn(database, alterAdd.group(1), alterAdd.group(2), alterAdd.group(3));
            }
            Matcher insert = INSERT.matcher(statement);
            if (insert.matches()) {
                if (!connection.details().writeEnabled()) {
                    return DaisyBaseSqlResult.failed("Database is read-only");
                }
                return insert(database, insert.group(1), insert.group(2), insert.group(3));
            }
            if (SHOW_TABLES.matcher(statement).matches()) {
                return showTables(database);
            }
            Matcher describe = DESCRIBE_TABLE.matcher(statement);
            if (describe.matches()) {
                return describeTable(database, describe.group(1));
            }
            Matcher select = SELECT.matcher(statement);
            if (select.matches()) {
                return select(
                        database,
                        select.group(1),
                        select.group(2),
                        select.group(3),
                        select.group(4),
                        select.group(5),
                        select.group(6));
            }
            Matcher update = UPDATE.matcher(statement);
            if (update.matches()) {
                if (!connection.details().writeEnabled()) {
                    return DaisyBaseSqlResult.failed("Database is read-only");
                }
                return update(database, update.group(1), update.group(2), update.group(3), update.group(4));
            }
            Matcher delete = DELETE.matcher(statement);
            if (delete.matches()) {
                if (!connection.details().writeEnabled()) {
                    return DaisyBaseSqlResult.failed("Database is read-only");
                }
                return delete(database, delete.group(1), delete.group(2), delete.group(3));
            }
            return DaisyBaseSqlResult.failed("Unsupported DaisyBase SQL statement");
        } catch (IllegalArgumentException error) {
            return DaisyBaseSqlResult.failed(error.getMessage());
        }
    }

    private DaisyBaseSqlResult createTable(ResourceRecord database, String tableName, String columnDefinitionText) {
        String table = normalizeIdentifier(tableName, "tableName");
        Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
        String schemaKey = schemaKey(table);
        if (attributes.containsKey(schemaKey)) {
            return DaisyBaseSqlResult.failed("Table already exists: " + table);
        }
        TableSchema schema = parseSchema(columnDefinitionText);
        attributes.put(schemaKey, schema.encode());
        attributes.put(rowsKey(table), "");
        resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
        return DaisyBaseSqlResult.update("created table " + table, 0);
    }

    private DaisyBaseSqlResult dropTable(ResourceRecord database, String tableName) {
        String table = normalizeIdentifier(tableName, "tableName");
        Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
        if (!attributes.containsKey(schemaKey(table))) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        attributes.remove(schemaKey(table));
        attributes.remove(rowsKey(table));
        resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
        return DaisyBaseSqlResult.update("dropped table " + table, 0);
    }

    private DaisyBaseSqlResult truncateTable(ResourceRecord database, String tableName) {
        String table = normalizeIdentifier(tableName, "tableName");
        Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
        if (!attributes.containsKey(schemaKey(table))) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        int deleted = decodeRows(attributes.get(rowsKey(table)), readSchema(attributes, table).columns().size()).size();
        attributes.put(rowsKey(table), "");
        resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
        return DaisyBaseSqlResult.update("truncated " + deleted + " row(s) from " + table, deleted);
    }

    private DaisyBaseSqlResult alterTableAddColumn(
            ResourceRecord database,
            String tableName,
            String columnName,
            String typeName) {
        String table = normalizeIdentifier(tableName, "tableName");
        String column = normalizeIdentifier(columnName, "columnName");
        String type = normalizeType(typeName);
        Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
        TableSchema schema = readSchema(attributes, table);
        if (schema == null) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        if (schema.indexOf(column) >= 0) {
            return DaisyBaseSqlResult.failed("Column already exists: " + column);
        }
        List<String> columns = new ArrayList<>(schema.columns());
        List<String> types = new ArrayList<>(schema.types());
        columns.add(column);
        types.add(type);
        TableSchema nextSchema = new TableSchema(columns, types);
        List<List<String>> rows = decodeRows(attributes.get(rowsKey(table)), schema.columns().size()).stream()
                .map(row -> {
                    List<String> next = new ArrayList<>(row);
                    next.add("");
                    return next;
                })
                .toList();
        attributes.put(schemaKey(table), nextSchema.encode());
        attributes.put(rowsKey(table), encodeRows(rows));
        resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
        return DaisyBaseSqlResult.update("added column " + column + " to " + table, 0);
    }

    private DaisyBaseSqlResult insert(ResourceRecord database, String tableName, String columnText, String valueText) {
        String table = normalizeIdentifier(tableName, "tableName");
        Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
        TableSchema schema = readSchema(attributes, table);
        if (schema == null) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        List<String> columns = splitCommaAware(columnText).stream()
                .map(column -> normalizeIdentifier(column, "columnName"))
                .toList();
        List<String> values = splitCommaAware(valueText).stream()
                .map(DaisyBaseConnector::parseValue)
                .toList();
        if (columns.size() != values.size()) {
            return DaisyBaseSqlResult.failed("INSERT column count does not match value count");
        }
        List<String> row = new ArrayList<>();
        for (int index = 0; index < schema.columns().size(); index++) {
            row.add("");
        }
        for (int index = 0; index < columns.size(); index++) {
            int schemaIndex = schema.indexOf(columns.get(index));
            if (schemaIndex < 0) {
                return DaisyBaseSqlResult.failed("Unknown column: " + columns.get(index));
            }
            String value = values.get(index);
            String type = schema.types().get(schemaIndex);
            if ("INT".equals(type) && !value.matches("-?\\d+")) {
                return DaisyBaseSqlResult.failed("Column " + columns.get(index) + " requires INT value");
            }
            row.set(schemaIndex, value);
        }
        attributes.put(rowsKey(table), appendRow(attributes.get(rowsKey(table)), row));
        resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
        return DaisyBaseSqlResult.update("inserted 1 row into " + table, 1);
    }

    private DaisyBaseSqlResult showTables(ResourceRecord database) {
        List<List<String>> rows = tables(database.resourceId()).stream()
                .map(table -> List.of(
                        table.name(),
                        Integer.toString(table.columns().size()),
                        Integer.toString(table.rowCount())))
                .toList();
        return DaisyBaseSqlResult.query(List.of("table", "columns", "rows"), rows);
    }

    private DaisyBaseSqlResult describeTable(ResourceRecord database, String tableName) {
        String table = normalizeIdentifier(tableName, "tableName");
        TableSchema schema = readSchema(database.attributes(), table);
        if (schema == null) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        List<List<String>> rows = new ArrayList<>();
        for (int index = 0; index < schema.columns().size(); index++) {
            rows.add(List.of(schema.columns().get(index), schema.types().get(index)));
        }
        return DaisyBaseSqlResult.query(List.of("column", "type"), rows);
    }

    private DaisyBaseSqlResult select(
            ResourceRecord database,
            String columnText,
            String tableName,
            String whereColumnText,
            String whereValueText,
            String orderColumnText,
            String limitText) {
        String table = normalizeIdentifier(tableName, "tableName");
        TableSchema schema = readSchema(database.attributes(), table);
        if (schema == null) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        List<List<String>> storedRows = decodeRows(database.attributes().get(rowsKey(table)), schema.columns().size());
        if (whereColumnText != null) {
            String whereColumn = normalizeIdentifier(whereColumnText, "whereColumn");
            String whereValue = parseValue(whereValueText);
            int whereIndex = schema.indexOf(whereColumn);
            if (whereIndex < 0) {
                return DaisyBaseSqlResult.failed("Unknown WHERE column: " + whereColumn);
            }
            storedRows = storedRows.stream()
                    .filter(row -> row.get(whereIndex).equals(whereValue))
                    .toList();
        }
        String orderColumn = orderColumnText == null ? null : normalizeIdentifier(orderColumnText, "orderColumn");
        if (orderColumn != null) {
            int orderIndex = schema.indexOf(orderColumn);
            if (orderIndex < 0) {
                return DaisyBaseSqlResult.failed("Unknown ORDER BY column: " + orderColumn);
            }
            Comparator<List<String>> comparator = Comparator.comparing(row -> row.get(orderIndex));
            if ("INT".equals(schema.types().get(orderIndex))) {
                comparator = Comparator.comparingInt(row -> sortableInt(row.get(orderIndex)));
            }
            storedRows = storedRows.stream().sorted(comparator).toList();
        }
        if (limitText != null) {
            int limit = Integer.parseInt(limitText);
            storedRows = storedRows.stream().limit(limit).toList();
        }
        if ("COUNT(*)".equalsIgnoreCase(columnText.trim())) {
            return DaisyBaseSqlResult.query(
                    List.of("count"),
                    List.of(List.of(Integer.toString(storedRows.size()))));
        }

        List<String> selectedColumns;
        if ("*".equals(columnText.trim())) {
            selectedColumns = schema.columns();
        } else {
            selectedColumns = splitCommaAware(columnText).stream()
                    .map(column -> normalizeIdentifier(column, "columnName"))
                    .toList();
        }
        List<Integer> selectedIndexes = new ArrayList<>();
        for (String column : selectedColumns) {
            int index = schema.indexOf(column);
            if (index < 0) {
                return DaisyBaseSqlResult.failed("Unknown column: " + column);
            }
            selectedIndexes.add(index);
        }
        List<List<String>> projected = storedRows.stream()
                .map(row -> selectedIndexes.stream().map(row::get).toList())
                .toList();
        return DaisyBaseSqlResult.query(selectedColumns, projected);
    }

    private DaisyBaseSqlResult update(
            ResourceRecord database,
            String tableName,
            String assignmentsText,
            String whereColumnText,
            String whereValueText) {
        String table = normalizeIdentifier(tableName, "tableName");
        String whereColumn = normalizeIdentifier(whereColumnText, "whereColumn");
        String whereValue = parseValue(whereValueText);
        Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
        TableSchema schema = readSchema(attributes, table);
        if (schema == null) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        int whereIndex = schema.indexOf(whereColumn);
        if (whereIndex < 0) {
            return DaisyBaseSqlResult.failed("Unknown WHERE column: " + whereColumn);
        }
        Map<Integer, String> assignments = parseAssignments(assignmentsText, schema);
        List<List<String>> rows = decodeRows(attributes.get(rowsKey(table)), schema.columns().size());
        int updated = 0;
        List<List<String>> nextRows = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> nextRow = new ArrayList<>(row);
            if (row.get(whereIndex).equals(whereValue)) {
                assignments.forEach(nextRow::set);
                updated++;
            }
            nextRows.add(nextRow);
        }
        attributes.put(rowsKey(table), encodeRows(nextRows));
        resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
        return DaisyBaseSqlResult.update("updated " + updated + " row(s) in " + table, updated);
    }

    private DaisyBaseSqlResult delete(
            ResourceRecord database,
            String tableName,
            String whereColumnText,
            String whereValueText) {
        String table = normalizeIdentifier(tableName, "tableName");
        String whereColumn = normalizeIdentifier(whereColumnText, "whereColumn");
        String whereValue = parseValue(whereValueText);
        Map<String, String> attributes = new LinkedHashMap<>(database.attributes());
        TableSchema schema = readSchema(attributes, table);
        if (schema == null) {
            return DaisyBaseSqlResult.failed("Unknown table: " + table);
        }
        int whereIndex = schema.indexOf(whereColumn);
        if (whereIndex < 0) {
            return DaisyBaseSqlResult.failed("Unknown WHERE column: " + whereColumn);
        }
        List<List<String>> rows = decodeRows(attributes.get(rowsKey(table)), schema.columns().size());
        List<List<String>> keptRows = rows.stream()
                .filter(row -> !row.get(whereIndex).equals(whereValue))
                .toList();
        int deleted = rows.size() - keptRows.size();
        attributes.put(rowsKey(table), encodeRows(keptRows));
        resources.update(new ResourceRecord(database.resourceId(), database.providerId(), database.resourceTypeId(), attributes));
        return DaisyBaseSqlResult.update("deleted " + deleted + " row(s) from " + table, deleted);
    }

    private ResourceRecord requireDatabase(String databaseResourceId) {
        String id = Text.require(databaseResourceId, "databaseResourceId");
        ResourceRecord database = resources.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Database resource not found: " + id));
        if (!DaisyBaseProviderCatalog.PROVIDER_ID.equals(database.providerId())
                || !DaisyBaseProviderCatalog.DATABASE_RESOURCE_TYPE.equals(database.resourceTypeId())) {
            throw new IllegalArgumentException("Resource is not a DaisyBase database: " + id);
        }
        return database;
    }

    private TableDescription describe(Map<String, String> attributes, String table) {
        TableSchema schema = readSchema(attributes, table);
        int rowCount = schema == null
                ? 0
                : decodeRows(attributes.get(rowsKey(table)), schema.columns().size()).size();
        return new TableDescription(
                table,
                schema == null ? List.of() : schema.columns(),
                schema == null ? List.of() : schema.types(),
                rowCount);
    }

    private static TabularImportData parseImportData(String format, String payload) {
        String normalizedFormat = Text.require(format, "format").toLowerCase(Locale.ROOT);
        String text = Text.require(payload, "payload");
        return switch (normalizedFormat) {
            case "csv", "text/csv" -> parseCsvImport(text);
            case "json", "jsonl", "ndjson", "semiregular-json", "semiregularjson", "application/json" ->
                    parseSemiRegularJsonImport(text);
            default -> throw new IllegalArgumentException("Unsupported import format: " + format);
        };
    }

    private static TabularImportData parseCsvImport(String payload) {
        List<List<String>> records = parseCsv(payload);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("CSV import requires a header row");
        }
        List<String> columns = normalizeImportColumns(records.get(0));
        List<List<String>> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> record = records.get(index);
            if (record.stream().allMatch(String::isBlank)) {
                continue;
            }
            rows.add(expandRow(record, columns.size()));
        }
        return new TabularImportData("csv", columns, inferTypes(rows, columns.size()), rows);
    }

    private static TabularImportData parseSemiRegularJsonImport(String payload) {
        List<Map<String, String>> objects = extractJsonObjects(payload).stream()
                .map(DaisyBaseConnector::parseFlatJsonObject)
                .toList();
        if (objects.isEmpty()) {
            throw new IllegalArgumentException("JSON import requires at least one object");
        }
        List<String> sourceColumns = new ArrayList<>();
        for (Map<String, String> object : objects) {
            for (String key : object.keySet()) {
                if (!sourceColumns.contains(key)) {
                    sourceColumns.add(key);
                }
            }
        }
        List<String> columns = normalizeImportColumns(sourceColumns);
        Map<String, String> columnMap = new LinkedHashMap<>();
        for (int index = 0; index < sourceColumns.size(); index++) {
            columnMap.put(sourceColumns.get(index), columns.get(index));
        }
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, String> object : objects) {
            List<String> row = new ArrayList<>();
            for (String sourceColumn : sourceColumns) {
                row.add(object.getOrDefault(sourceColumn, ""));
            }
            rows.add(row);
        }
        return new TabularImportData("semiregular-json", new ArrayList<>(columnMap.values()),
                inferTypes(rows, columns.size()), rows);
    }

    private static TableSchema parseSchema(String columnDefinitionText) {
        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();
        for (String definition : splitCommaAware(columnDefinitionText)) {
            String[] parts = definition.trim().split("\\s+");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid column definition: " + definition);
            }
            String column = normalizeIdentifier(parts[0], "columnName");
            String type = normalizeType(parts[1]);
            if (columns.contains(column)) {
                throw new IllegalArgumentException("Duplicate column: " + column);
            }
            columns.add(column);
            types.add(type);
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("CREATE TABLE requires at least one column");
        }
        return new TableSchema(columns, types);
    }

    private static TableSchema readSchema(Map<String, String> attributes, String table) {
        String encoded = attributes.get(schemaKey(table));
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        return TableSchema.decode(encoded);
    }

    private static String normalizeStatement(String sql) {
        String statement = Text.require(sql, "sql");
        while (statement.endsWith(";")) {
            statement = statement.substring(0, statement.length() - 1).trim();
        }
        return statement;
    }

    private static String normalizeIdentifier(String value, String label) {
        String identifier = Text.require(value, label);
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(label + " must start with a letter and contain only letters, digits, or underscores");
        }
        return identifier.toLowerCase(Locale.ROOT);
    }

    private static String normalizeImportIdentifier(String value, int index) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            trimmed = "column_" + (index + 1);
        }
        StringBuilder identifier = new StringBuilder();
        for (int cursor = 0; cursor < trimmed.length(); cursor++) {
            char ch = trimmed.charAt(cursor);
            if (Character.isLetterOrDigit(ch)) {
                identifier.append(Character.toLowerCase(ch));
            } else if (identifier.length() == 0 || identifier.charAt(identifier.length() - 1) != '_') {
                identifier.append('_');
            }
        }
        while (identifier.length() > 0 && identifier.charAt(identifier.length() - 1) == '_') {
            identifier.setLength(identifier.length() - 1);
        }
        if (identifier.length() == 0) {
            identifier.append("column_").append(index + 1);
        }
        if (!Character.isLetter(identifier.charAt(0))) {
            identifier.insert(0, "c_");
        }
        return normalizeIdentifier(identifier.toString(), "columnName");
    }

    private static List<String> normalizeImportColumns(List<String> rawColumns) {
        if (rawColumns.isEmpty()) {
            throw new IllegalArgumentException("Import requires at least one column");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> columns = new ArrayList<>();
        for (int index = 0; index < rawColumns.size(); index++) {
            String candidate = normalizeImportIdentifier(rawColumns.get(index), index);
            String unique = candidate;
            int suffix = 2;
            while (seen.contains(unique)) {
                unique = candidate + "_" + suffix++;
            }
            seen.add(unique);
            columns.add(unique);
        }
        return columns;
    }

    private static String normalizeType(String typeName) {
        String type = Text.require(typeName, "type").toUpperCase(Locale.ROOT);
        if (!"INT".equals(type) && !"TEXT".equals(type)) {
            throw new IllegalArgumentException("Unsupported DaisyBase column type: " + type);
        }
        return type;
    }

    private static List<String> inferTypes(List<List<String>> rows, int columnCount) {
        List<String> types = new ArrayList<>();
        for (int column = 0; column < columnCount; column++) {
            int index = column;
            boolean integer = !rows.isEmpty() && rows.stream()
                    .map(row -> row.get(index))
                    .allMatch(value -> !value.isBlank() && value.matches("-?\\d+"));
            types.add(integer ? "INT" : "TEXT");
        }
        return types;
    }

    private static List<String> expandRow(List<String> row, int columnCount) {
        List<String> expanded = new ArrayList<>();
        for (int index = 0; index < columnCount; index++) {
            expanded.add(index < row.size() ? row.get(index) : "");
        }
        return expanded;
    }

    private static int sortableInt(String value) {
        if (value == null || value.isBlank()) {
            return Integer.MIN_VALUE;
        }
        return Integer.parseInt(value);
    }

    private static List<String> splitCommaAware(String value) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\'') {
                current.append(ch);
                if (quoted && index + 1 < value.length() && value.charAt(index + 1) == '\'') {
                    current.append(value.charAt(++index));
                    continue;
                }
                quoted = !quoted;
                continue;
            }
            if (ch == ',' && !quoted) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (quoted) {
            throw new IllegalArgumentException("Unterminated quoted string");
        }
        parts.add(current.toString().trim());
        return parts;
    }

    private static List<List<String>> parseCsv(String value) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < value.length() && value.charAt(index + 1) == '"') {
                    cell.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (ch == ',' && !quoted) {
                row.add(cell.toString());
                cell.setLength(0);
                continue;
            }
            if ((ch == '\n' || ch == '\r') && !quoted) {
                if (ch == '\r' && index + 1 < value.length() && value.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
                continue;
            }
            cell.append(ch);
        }
        if (quoted) {
            throw new IllegalArgumentException("CSV import contains an unterminated quoted value");
        }
        row.add(cell.toString());
        if (!row.stream().allMatch(String::isBlank) || rows.isEmpty()) {
            rows.add(row);
        }
        return rows;
    }

    private static List<String> extractJsonObjects(String payload) {
        String text = payload.trim();
        List<String> objects = new ArrayList<>();
        int start = -1;
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    quoted = false;
                }
                continue;
            }
            if (ch == '"') {
                quoted = true;
                escaped = false;
                continue;
            }
            if (ch == '{') {
                if (depth == 0) {
                    start = index;
                }
                depth++;
                continue;
            }
            if (ch == '}') {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("JSON import has unmatched closing brace");
                }
                if (depth == 0 && start >= 0) {
                    objects.add(text.substring(start, index + 1));
                    start = -1;
                }
            }
        }
        if (quoted || depth != 0) {
            throw new IllegalArgumentException("JSON import is incomplete");
        }
        return objects;
    }

    private static Map<String, String> parseFlatJsonObject(String objectText) {
        String text = objectText.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw new IllegalArgumentException("JSON import expects object rows");
        }
        Map<String, String> values = new LinkedHashMap<>();
        int index = 1;
        int end = text.length() - 1;
        while (index < end) {
            index = skipJsonWhitespaceAndCommas(text, index, end);
            if (index >= end) {
                break;
            }
            JsonToken key = readJsonKey(text, index, end);
            index = skipJsonWhitespace(text, key.nextIndex(), end);
            if (index >= end || (text.charAt(index) != ':' && text.charAt(index) != '=')) {
                throw new IllegalArgumentException("JSON import expects ':' after key " + key.value());
            }
            index = skipJsonWhitespace(text, index + 1, end);
            JsonToken value = readJsonValue(text, index, end);
            values.put(key.value(), value.value());
            index = value.nextIndex();
        }
        return values;
    }

    private static int skipJsonWhitespaceAndCommas(String text, int index, int end) {
        int cursor = index;
        while (cursor < end && (Character.isWhitespace(text.charAt(cursor)) || text.charAt(cursor) == ',')) {
            cursor++;
        }
        return cursor;
    }

    private static int skipJsonWhitespace(String text, int index, int end) {
        int cursor = index;
        while (cursor < end && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static JsonToken readJsonKey(String text, int index, int end) {
        if (text.charAt(index) == '"') {
            return readJsonString(text, index, end);
        }
        int cursor = index;
        while (cursor < end && text.charAt(cursor) != ':' && text.charAt(cursor) != '=') {
            cursor++;
        }
        String key = text.substring(index, cursor).trim();
        if (key.isBlank()) {
            throw new IllegalArgumentException("JSON import contains a blank key");
        }
        return new JsonToken(key, cursor);
    }

    private static JsonToken readJsonValue(String text, int index, int end) {
        if (index >= end) {
            return new JsonToken("", index);
        }
        char first = text.charAt(index);
        if (first == '"') {
            return readJsonString(text, index, end);
        }
        if (first == '{' || first == '[') {
            int close = matchingJsonClose(text, index, end);
            return new JsonToken(text.substring(index, close + 1), close + 1);
        }
        int cursor = index;
        while (cursor < end && text.charAt(cursor) != ',') {
            cursor++;
        }
        String raw = text.substring(index, cursor).trim();
        if ("null".equalsIgnoreCase(raw)) {
            raw = "";
        }
        return new JsonToken(raw, cursor);
    }

    private static JsonToken readJsonString(String text, int index, int end) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int cursor = index + 1; cursor < end; cursor++) {
            char ch = text.charAt(cursor);
            if (escaped) {
                switch (ch) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> {
                        if (cursor + 4 >= end) {
                            throw new IllegalArgumentException("JSON import contains an incomplete unicode escape");
                        }
                        String hex = text.substring(cursor + 1, cursor + 5);
                        try {
                            value.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException error) {
                            throw new IllegalArgumentException("JSON import contains an invalid unicode escape: " + hex);
                        }
                        cursor += 4;
                    }
                    default -> value.append(ch);
                }
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                return new JsonToken(value.toString(), cursor + 1);
            }
            value.append(ch);
        }
        throw new IllegalArgumentException("JSON import contains an unterminated string");
    }

    private static int matchingJsonClose(String text, int index, int end) {
        char open = text.charAt(index);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int cursor = index; cursor < end; cursor++) {
            char ch = text.charAt(cursor);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    quoted = false;
                }
                continue;
            }
            if (ch == '"') {
                quoted = true;
                escaped = false;
                continue;
            }
            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return cursor;
                }
            }
        }
        throw new IllegalArgumentException("JSON import contains an incomplete nested value");
    }

    private static Map<Integer, String> parseAssignments(String assignmentsText, TableSchema schema) {
        Map<Integer, String> assignments = new LinkedHashMap<>();
        for (String assignment : splitCommaAware(assignmentsText)) {
            String[] parts = assignment.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid assignment: " + assignment);
            }
            String column = normalizeIdentifier(parts[0], "assignmentColumn");
            int index = schema.indexOf(column);
            if (index < 0) {
                throw new IllegalArgumentException("Unknown assignment column: " + column);
            }
            String value = parseValue(parts[1]);
            if ("INT".equals(schema.types().get(index)) && !value.matches("-?\\d+")) {
                throw new IllegalArgumentException("Column " + column + " requires INT value");
            }
            assignments.put(index, value);
        }
        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("UPDATE requires at least one assignment");
        }
        return assignments;
    }

    private static String parseValue(String token) {
        String value = token.trim();
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
            return value.substring(1, value.length() - 1).replace("''", "'");
        }
        return value;
    }

    private static String appendRow(String existingRows, List<String> row) {
        String encoded = row.stream()
                .map(DaisyBaseConnector::encodeValue)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        if (existingRows == null || existingRows.isBlank()) {
            return encoded;
        }
        return existingRows + ";" + encoded;
    }

    private static List<List<String>> decodeRows(String encodedRows, int expectedColumns) {
        if (encodedRows == null || encodedRows.isBlank()) {
            return List.of();
        }
        List<List<String>> rows = new ArrayList<>();
        for (String encodedRow : encodedRows.split(";")) {
            String[] parts = encodedRow.split(",", -1);
            if (parts.length != expectedColumns) {
                throw new IllegalStateException("Stored DaisyBase row does not match schema");
            }
            List<String> row = new ArrayList<>();
            for (String part : parts) {
                row.add(decodeValue(part));
            }
            rows.add(row);
        }
        return rows;
    }

    private static String encodeRows(List<List<String>> rows) {
        return rows.stream()
                .map(row -> row.stream()
                        .map(DaisyBaseConnector::encodeValue)
                        .reduce((left, right) -> left + "," + right)
                        .orElse(""))
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private static String encodeValue(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeValue(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String schemaKey(String table) {
        return SCHEMA_PREFIX + table + SCHEMA_SUFFIX;
    }

    private static String rowsKey(String table) {
        return SCHEMA_PREFIX + table + ROWS_SUFFIX;
    }

    public record TableDescription(String name, List<String> columns, List<String> types, int rowCount) {
        public TableDescription {
            name = normalizeIdentifier(name, "tableName");
            columns = List.copyOf(Objects.requireNonNull(columns, "columns must not be null"));
            types = List.copyOf(Objects.requireNonNull(types, "types must not be null"));
            if (columns.size() != types.size()) {
                throw new IllegalArgumentException("columns and types must have the same size");
            }
            if (rowCount < 0) {
                throw new IllegalArgumentException("rowCount must not be negative");
            }
        }
    }

    private record TabularImportData(String format, List<String> columns, List<String> types, List<List<String>> rows) {
        private TabularImportData {
            format = Text.require(format, "format");
            columns = List.copyOf(columns);
            types = List.copyOf(types);
            rows = rows.stream().map(List::copyOf).toList();
            if (columns.size() != types.size()) {
                throw new IllegalArgumentException("columns and types must have the same size");
            }
            for (List<String> row : rows) {
                if (row.size() != columns.size()) {
                    throw new IllegalArgumentException("import row does not match column count");
                }
            }
        }
    }

    private record JsonToken(String value, int nextIndex) {
    }

    private record TableSchema(List<String> columns, List<String> types) {
        private TableSchema {
            columns = List.copyOf(columns);
            types = List.copyOf(types);
            if (columns.size() != types.size()) {
                throw new IllegalArgumentException("columns and types must have the same size");
            }
        }

        int indexOf(String column) {
            return columns.indexOf(column);
        }

        String encode() {
            List<String> definitions = new ArrayList<>();
            for (int index = 0; index < columns.size(); index++) {
                definitions.add(columns.get(index) + ":" + types.get(index));
            }
            return String.join(",", definitions);
        }

        static TableSchema decode(String value) {
            List<String> columns = new ArrayList<>();
            List<String> types = new ArrayList<>();
            for (String definition : value.split(",")) {
                String[] parts = definition.split(":", -1);
                if (parts.length != 2) {
                    throw new IllegalStateException("Invalid stored DaisyBase schema");
                }
                columns.add(parts[0]);
                types.add(parts[1]);
            }
            return new TableSchema(columns, types);
        }
    }
}
