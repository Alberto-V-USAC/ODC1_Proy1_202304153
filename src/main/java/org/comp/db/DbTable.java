package org.comp.db;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.control.Option;

import java.util.function.Predicate;


public class DbTable {
    public enum RecordError {
        BadTyping,
        ExtraFields,
        MissingField,
    }

    public enum UpdateError {
        BadTyping,
        BadFilter,
        ExtraFields,
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

    public HashSet<HashMap<String, DbType>> read() {
        return records;
    }

    public HashSet<HashMap<String, DbType>> read(
            Predicate<HashMap<String, DbType>> filter
    ) {
        return records.filter(filter);
    }

    public Option<UpdateError> update(HashMap<String, DbType> newRecord) {
        return update(newRecord, ignored -> true);
    }

    public Option<UpdateError> update(
            HashMap<String, DbType> newRecord,
            Predicate<HashMap<String, DbType>> filter
    ) {
        // Need to check for contention but not equality
        // record.keys ⊆ schema.keys
        for (var key : newRecord.keySet()) {
            if (!schema.containsKey(key))
                return Option.of(UpdateError.ExtraFields);

            Class<?> classSchema = schema.get(key).get();
            Class<?> classRecord = newRecord.get(key).get().getClass();

            if (classSchema != classRecord)
                return Option.of(UpdateError.BadTyping);
        }

        // Filter according to predicate
        records = records
                .map(rec -> filter.test(rec) ? rec.merge(newRecord, (v1, v2) -> v2) : rec);

        return Option.none();
    }

    public void clear() {
        records = HashSet.empty();
    }
}
