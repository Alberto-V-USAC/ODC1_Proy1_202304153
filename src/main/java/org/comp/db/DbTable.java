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

    static UpdateError recordErrorToUpdateError(RecordError error) {
        return switch(error) {
            case RecordError.BadTyping -> UpdateError.BadTyping;
            case RecordError.ExtraFields, RecordError.MissingField -> UpdateError.ExtraFields;
        };
    }

    String tableName;
    HashMap<String, Class<?>> schema;
    HashSet<HashMap<String, DbType>> records = HashSet.empty();

    public DbTable(String tableName, HashMap<String, Class<?>> schema) {
        this.tableName = tableName;
        this.schema = schema;
    }

    public Option<RecordError> checkRecord(HashMap<String, DbType> record) {
        return checkRecord(record, true);
    }

    public Option<RecordError> checkRecord(HashMap<String, DbType> record, boolean checkEquality) {
        // Key validation by double contention
        // record.keys ⊆ schema.keys
        for (var key : record.keySet()) {
            if (!schema.containsKey(key))
                return Option.of(RecordError.ExtraFields);
        }

        // schema.keys ⊆ record.keys
        if (checkEquality) {
            for (var key : schema.keySet()) {
                if (!record.containsKey(key))
                    return Option.of(RecordError.MissingField);
            }
        }

        // Now, check the correct typings
        for (var key : record.keySet()) {
            Class<?> classSchema = schema.get(key).get();
            Class<?> classRecord = record.get(key).get().getClass();

            if (classSchema != classRecord)
                return Option.of(RecordError.BadTyping);
        }

        return Option.none();
    }

    public Option<RecordError> add(HashMap<String, DbType> record) {
        var maybeError = checkRecord(record);

        if (maybeError.isEmpty())
            records = records.add(record);

        return maybeError;
    }

    public Option<RecordError> addNullable(HashMap<String ,DbType> record) {
        var maybeError = checkRecord(record, false);

        if (maybeError.isEmpty())
            records = records.add(record);

        return maybeError;
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
        var err = this.checkRecord(newRecord, false);
        if (err.isDefined())
            return err.map(DbTable::recordErrorToUpdateError);

        // Filter according to predicate
        records = records
                .map(rec -> filter.test(rec) ? rec.merge(newRecord, (v1, v2) -> v2) : rec);

        return Option.none();
    }

    public void clear() {
        records = HashSet.empty();
    }
}
