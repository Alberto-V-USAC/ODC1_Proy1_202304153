package org.comp.db;

import java.io.File;
import java.util.function.Predicate;

import io.vavr.collection.List;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.control.Either;
import io.vavr.control.Option;


public class Database {
    public enum TableError {
        BadSchema,
        ExistingTable,
        TableNotFound,
    }

    public enum AddError {
        TableNotFound,
        BadTyping,
        ExtraFields,
        IncompleteFields,
    }

    public enum UpdateError {
        TableNotFound,
        BadTyping,
        BadFilter,
        ExtraFields,
    }

    public static class RecordFilterer {
        private final HashSet<HashMap<String, DbType>> records;

        private RecordFilterer(HashSet<HashMap<String, DbType>> records) {
            this.records = records;
        }

        public static class Field {
            private final String fieldId;
            private final HashSet<HashMap<String, DbType>> records;

            private Field(String fieldId, HashSet<HashMap<String, DbType>> records) {
                this.fieldId = fieldId;
                this.records = records;
            }

            public RecordFilterer filter(Predicate<DbType> filter) {
                var newRecords = records
                        .filter(record -> record.get(fieldId).isDefined())
                        .filter(record -> filter.test(record.get(fieldId).get()));

                return new RecordFilterer(newRecords);
            }
        }

        public Field field(String fieldId) {
            return new Field(fieldId, records);
        }

        public HashSet<HashMap<String, DbType>> collect() {
            return records;
        }
    }

    AddError recordErrorToAddError(DbTable.RecordError error) {
        // Just casting errors
        return switch (error) {
            case DbTable.RecordError.BadTyping -> AddError.BadTyping;
            case DbTable.RecordError.ExtraFields -> AddError.ExtraFields;
            case DbTable.RecordError.MissingField -> AddError.IncompleteFields;
        };
    }

    UpdateError tableUpdateErrorToUpdateError(DbTable.UpdateError error) {
        return switch (error) {
            case DbTable.UpdateError.BadTyping -> UpdateError.BadTyping;
            case DbTable.UpdateError.ExtraFields -> UpdateError.ExtraFields;
            case DbTable.UpdateError.BadFilter -> UpdateError.BadFilter;
        };
    }

    public String dbName;
    public String storePath;
    private List<DbTable> tables = List.empty();

    public Database(String dbName, String storePath) {
        this.dbName = dbName;
        this.storePath = storePath;

        if (new File(storePath).exists())
            loadFromJson(storePath);
    }

    public void persist() {
        saveToJson(storePath);
    }

    public Option<TableError> addTable(String tableName, HashMap<String, Class<?>> schema) {
        // Verify schema
        for (var value : schema.valuesIterator()) {
            List<Class<?>> interfaces = List.of(value.getInterfaces());

            if (!interfaces.contains(DbType.class))
                return Option.of(TableError.BadSchema);
        }

        // Verify repetition
        if (tables.map(table -> table.tableName).contains(tableName))
            return Option.of(TableError.ExistingTable);

        tables = tables.push(new DbTable(tableName, schema));
        return Option.none();
    }

    public Either<HashSet<HashMap<String, DbType>>, TableError> readTable(String tableName) {
        for (var table : this.tables) {
            if (table.tableName.equals(tableName))
                return Either.left(table.read());
        }

        return Either.right(TableError.TableNotFound);
    }

    public Either<HashSet<HashMap<String, DbType>>, TableError> readTable(
        String tableName,
        Predicate<HashMap<String, DbType>> filter
    ) {
        for (var table : this.tables) {
            if (table.tableName.equals(tableName))
                return Either.left(table.read(filter));
        }

        return Either.right(TableError.TableNotFound);
    }

    public Either<RecordFilterer, TableError> readFilter(String tableName) {
        for (var table : this.tables) {
            if (table.tableName.equals(tableName))
                return Either.left(new RecordFilterer(table.records));
        }

        return Either.right(TableError.TableNotFound);
    }

    public Option<AddError> addRecord(
        String tableName,
        HashMap<String, DbType> record
    ) {
        for (var table : this.tables) {
            if (table.tableName.equals(tableName)) {
                return table
                        .addNullable(record)
                        .map(this::recordErrorToAddError);
            }
        }

        return Option.of(AddError.TableNotFound);
    }

    public Option<AddError> addRecordExact(
        String tableName,
        HashMap<String, DbType> record
    ) {
        for (var table : this.tables) {
            if (table.tableName.equals(tableName)) {
                return table
                        .add(record)
                        .map(this::recordErrorToAddError);
            }
        }

        return Option.of(AddError.TableNotFound);
    }

    public Option<UpdateError> updateRecord(
        String tableName,
        HashMap<String, DbType> newRecord
    ) {
        return updateRecord(tableName, newRecord, ignored -> true);
    }

    public Option<UpdateError> updateRecord(
        String tableName,
        HashMap<String, DbType> newRecord,
        Predicate<HashMap<String, DbType>> filter
    ) {
        for (var table : this.tables) {
            if (table.tableName.equals(tableName))
                return table
                        .update(newRecord, filter)
                        .map(this::tableUpdateErrorToUpdateError);
        }

        return Option.of(UpdateError.TableNotFound);
    }

    public void dropTable(String tableName) {
        tables = tables.removeFirst(table -> table.tableName.equals(tableName));
    }

    public String formatRecord(String tableName, HashMap<String, DbType> record, List<String> fieldsToDisplay) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("=========\n");

        // Look up the table to ensure it exists (maintains original behavior)
        var schema = tables.find(table -> table.tableName.equals(tableName)).get().schema;
        if (fieldsToDisplay.isEmpty())
            fieldsToDisplay = schema.keySet().toList();

        // Iterate over the provided list of fields instead of the entire schema
        for (String key : fieldsToDisplay) {
            var maybe = record.get(key);

            if (maybe.isEmpty()) {
                stringBuilder.append(key).append(" : null\n");
            } else {
                stringBuilder.append(key).append(" : ").append(maybe.get().format()).append("\n");
            }
        }
        stringBuilder.append("=========\n");

        return stringBuilder.toString();
    }

    public void saveToJson(String storePath) {
        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"database\": \"").append(escapeJson(this.dbName)).append("\",\n");
        json.append("  \"tables\": {\n");

        boolean firstTable = true;
        for (DbTable table : this.tables) {
            if (!firstTable) {
                json.append(",\n");
            }
            firstTable = false;

            json.append("    \"").append(escapeJson(table.tableName)).append("\": {\n");

            // 1. Serialize Schema
            json.append("      \"schema\": {");
            boolean firstSchema = true;
            for (var entry : table.schema) {
                if (!firstSchema) json.append(", ");
                firstSchema = false;
                // Using getSimpleName() extracts "Integer", "String", etc. from the Class<?>
                json.append("\"").append(escapeJson(entry._1)).append("\": \"")
                        .append(entry._2.getSimpleName()).append("\"");
            }
            json.append("},\n");

            // 2. Serialize Records
            json.append("      \"records\": [\n");
            boolean firstRecord = true;
            for (var record : table.records) {
                if (!firstRecord) json.append(",\n");
                firstRecord = false;
                json.append("        ").append(recordToJson(record));
            }
            json.append("\n      ]\n");
            json.append("    }");
        }

        json.append("\n  }\n");
        json.append("}\n");

        // Write the built JSON string to the specified file path
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(storePath), json.toString());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save database to JSON: " + e.getMessage(), e);
        }
    }

    private String recordToJson(HashMap<String, DbType> record) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (var entry : record) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(escapeJson(entry._1)).append("\": ").append(dbTypeToJson(entry._2));
        }
        sb.append("}");
        return sb.toString();
    }

    private String dbTypeToJson(DbType type) {
        // Java 21 Pattern Matching for switch makes unpacking the sealed interface very clean
        return switch (type) {
            case DbType.Integer t -> java.lang.String.valueOf(t.get());
            case DbType.Float t -> java.lang.String.valueOf(t.get());
            case DbType.Bool t -> java.lang.String.valueOf(t.get());
            case DbType.String t -> "\"" + escapeJson(t.get()) + "\"";
            case DbType.Array t -> {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                for (DbType item : t.get()) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append(dbTypeToJson(item));
                }
                sb.append("]");
                yield sb.toString();
            }
            case DbType.DbObject t -> {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                boolean first = true;
                for (var entry : t.get()) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append("\"").append(escapeJson(entry._1)).append("\": ").append(dbTypeToJson(entry._2));
                }
                sb.append("}");
                yield sb.toString();
            }
        };
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void loadFromJson(String storePath) {
        try {
            String jsonStr = java.nio.file.Files.readString(java.nio.file.Path.of(storePath));

            // Parse raw JSON into standard java.util Maps and Lists
            SimpleJsonParser parser = new SimpleJsonParser(jsonStr);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> root = (java.util.Map<String, Object>) parser.parse();

            this.dbName = (String) root.get("database");
            this.tables = List.empty(); // Clear current tables

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> tablesMap = (java.util.Map<String, Object>) root.get("tables");

            for (var entry : tablesMap.entrySet()) {
                String tableName = entry.getKey();

                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> tableData = (java.util.Map<String, Object>) entry.getValue();

                // 1. Reconstruct Schema
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> schemaMap = (java.util.Map<String, Object>) tableData.get("schema");
                HashMap<String, Class<?>> schema = HashMap.empty();

                for (var schemaEntry : schemaMap.entrySet()) {
                    schema = schema.put(schemaEntry.getKey(), stringToDbTypeClass((String) schemaEntry.getValue()));
                }

                DbTable table = new DbTable(tableName, schema);

                // 2. Reconstruct Records
                @SuppressWarnings("unchecked")
                java.util.List<Object> recordsList = (java.util.List<Object>) tableData.get("records");

                for (Object recObj : recordsList) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> recMap = (java.util.Map<String, Object>) recObj;
                    HashMap<String, DbType> record = HashMap.empty();

                    for (var recEntry : recMap.entrySet()) {
                        String colName = recEntry.getKey();
                        Object rawValue = recEntry.getValue();

                        // Look up expected class so we cast Double to Float if necessary
                        Class<?> expectedClass = schema.get(colName).getOrElse((Class<?>) null);
                        DbType dbVal = convertToDbType(rawValue, expectedClass);

                        if (dbVal != null) {
                            record = record.put(colName, dbVal);
                        }
                    }
                    table.addNullable(record);
                }

                // Append table to Vavr List
                this.tables = this.tables.append(table);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load database from JSON: " + e.getMessage(), e);
        }
    }

    private Class<?> stringToDbTypeClass(String typeName) {
        return switch (typeName) {
            case "Integer" -> DbType.Integer.class;
            case "Float" -> DbType.Float.class;
            case "Bool" -> DbType.Bool.class;
            case "String" -> DbType.String.class;
            case "Array" -> DbType.Array.class;
            case "DbObject" -> DbType.DbObject.class;
            default -> throw new IllegalArgumentException("Unknown DbType schema class: " + typeName);
        };
    }

    private DbType convertToDbType(Object parsedValue, Class<?> expectedClass) {
        if (parsedValue == null) return null;

        // Recursively handle nested Arrays
        if (parsedValue instanceof java.util.List<?> list) {
            List<DbType> vavrList = List.empty();
            for (Object item : list) {
                vavrList = vavrList.append(convertToDbType(item, null));
            }
            return DbType.from_list(vavrList);
        }

        // Recursively handle nested DbObjects
        if (parsedValue instanceof java.util.Map<?, ?> map) {
            HashMap<String, DbType> vavrMap = HashMap.empty();
            for (var entry : map.entrySet()) {
                vavrMap = vavrMap.put((String) entry.getKey(), convertToDbType(entry.getValue(), null));
            }
            return DbType.from_map(vavrMap);
        }

        // Handle Numbers, utilizing expected schema to enforce Integer vs Float bounds
        if (parsedValue instanceof Number n) {
            if (expectedClass == DbType.Integer.class) return DbType.from_raw(n.intValue());
            if (expectedClass == DbType.Float.class) return DbType.from_raw(n.floatValue());

            // Fallback inference if inside an array without strict schema typing
            if (parsedValue instanceof Double) return DbType.from_raw(n.floatValue());
            return DbType.from_raw(n.intValue());
        }

        // Delegate Strings, Booleans to existing factory method
        Option<DbType> maybePrimitive = DbType.from_primitive(parsedValue);
        if (maybePrimitive.isDefined()) {
            return maybePrimitive.get();
        }

        throw new IllegalArgumentException("Unsupported JSON mapping value: " + parsedValue);
    }

    // --- Private JSON Parser to avoid external library dependencies ---
    private static class SimpleJsonParser {
        private final String json;
        private int pos = 0;

        public SimpleJsonParser(String json) { this.json = json; }

        public Object parse() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);

            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't') { pos += 4; return true; }
            if (c == 'f') { pos += 5; return false; }
            if (c == 'n') { pos += 4; return null; }
            return parseNumber();
        }

        private java.util.Map<String, Object> parseObject() {
            var map = new java.util.LinkedHashMap<String, Object>();
            pos++; // skip '{'
            skipWhitespace();
            if (json.charAt(pos) == '}') { pos++; return map; }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                pos++; // skip ':'
                Object value = parse();
                map.put(key, value);
                skipWhitespace();
                if (json.charAt(pos) == '}') { pos++; break; }
                pos++; // skip ','
            }
            return map;
        }

        private java.util.List<Object> parseArray() {
            var list = new java.util.ArrayList<Object>();
            pos++; // skip '['
            skipWhitespace();
            if (json.charAt(pos) == ']') { pos++; return list; }

            while (true) {
                list.add(parse());
                skipWhitespace();
                if (json.charAt(pos) == ']') { pos++; break; }
                pos++; // skip ','
            }
            return list;
        }

        private String parseString() {
            pos++; // skip starting '"'
            StringBuilder sb = new StringBuilder();
            while (json.charAt(pos) != '"') {
                char c = json.charAt(pos++);
                if (c == '\\') {
                    char esc = json.charAt(pos++);
                    switch (esc) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            pos++; // skip ending '"'
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < json.length() && "-0123456789.eE+".indexOf(json.charAt(pos)) != -1) {
                pos++;
            }
            String s = json.substring(start, pos);
            if (s.contains(".") || s.contains("e") || s.contains("E")) return Double.parseDouble(s);
            return Integer.parseInt(s);
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
}
