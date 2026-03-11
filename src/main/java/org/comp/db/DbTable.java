package org.comp.db;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.control.Option;


public class DbTable {
    public enum RecordError {
        BadTyping,
        ExtraFields,
        MissingField,
    }

    String tableName;
    HashMap<String, Class<?>> schema;
    HashSet<HashMap<String, DbType>> records = HashSet.empty();

    public DbTable(String tableName, HashMap<String, Class<?>> schema) {
        this.tableName = tableName;
        this.schema = schema;
    }

    public Option<RecordError> checkRecord(HashMap<String, DbType> record) {
        // Key validation by double contention
        // record.keys ⊆ schema.keys
        for (var key : record.keySet()) {
            if (!schema.containsKey(key))
                return Option.of(RecordError.ExtraFields);
        }

        // schema.keys ⊆ record.keys
        for (var key : schema.keySet()) {
            if (!record.containsKey(key))
                return Option.of(RecordError.MissingField);
        }

        // Now, check the correct typings
        for (var key : schema.keySet()) {
            Class<?> classSchema = schema.get(key).get();
            Class<?> classRecord = record.get(key).get().getClass();

            if (classSchema != classRecord)
                return Option.of(RecordError.BadTyping);
        }

        return Option.none();
    }

    public Option<RecordError> add(HashMap<String, DbType> record) {
        var maybeError = checkRecord(record);

        if (checkRecord(record).isDefined())
            return maybeError;
        else {
            records = records.add(record);
            return Option.none();
        }
    }

    public void clear() {
        records = HashSet.empty();
    }
}
