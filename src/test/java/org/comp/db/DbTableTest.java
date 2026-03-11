package org.comp.db;

import io.vavr.collection.HashMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbTableTest {
    HashMap<String, Class<?>> basicSchema = HashMap.of(
            "id", DbType.Integer.class,
            "foo", DbType.String.class,
            "bar", DbType.Float.class,
            "spam", DbType.Bool.class
    );

    @Test
    void checkRecord() {
        var table = new DbTable("table", basicSchema);

        // Missing fields case
        HashMap<String, DbType> record0 = HashMap.of(
                "id", DbType.from_raw(32),
                "spam", DbType.from_raw(false)
        );
        assertTrue(table.checkRecord(record0).isDefined());
        assertEquals(
                table.checkRecord(record0).get(),
                DbTable.RecordError.MissingField
        );

        // Extra fields
        HashMap<String, DbType> record1 = HashMap.of(
                "id", DbType.from_raw(32),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false),
                "extra", DbType.from_raw(67)
        );
        assertTrue(table.checkRecord(record1).isDefined());
        assertEquals(
                table.checkRecord(record1).get(),
                DbTable.RecordError.ExtraFields
        );

        // Bad typing 1
        HashMap<String, DbType> record2 = HashMap.of(
                "id", DbType.from_raw(32),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42),
                "spam", DbType.from_raw(false)
        );
        assertTrue(table.checkRecord(record2).isDefined());
        assertEquals(
                table.checkRecord(record2).get(),
                DbTable.RecordError.BadTyping
        );

        // Bad typing 2
        HashMap<String, DbType> record3 = HashMap.of(
                "id", DbType.from_raw(32),
                "foo", DbType.from_raw(42),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false)
        );
        assertTrue(table.checkRecord(record3).isDefined());
        assertEquals(
                table.checkRecord(record3).get(),
                DbTable.RecordError.BadTyping
        );

        // Correct case
        HashMap<String, DbType> record4 = HashMap.of(
                "id", DbType.from_raw(32),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false)
        );
        assertTrue(table.checkRecord(record4).isEmpty());
    }

    @Test
    void add() {
        var table = new DbTable("table", basicSchema);

        var error = table.add(HashMap.of(
                "id", DbType.from_raw(1),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false)
        ));
        assertTrue(error.isEmpty());

        error = table.add(HashMap.of(
                "id", DbType.from_raw(2),
                "foo", DbType.from_raw("this is more text"),
                "bar", DbType.from_raw(13.0f),
                "spam", DbType.from_raw(false)
        ));
        assertTrue(error.isEmpty());

        error = table.add(HashMap.of(
                "id", DbType.from_raw(1),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false)
        ));
        assertTrue(error.isEmpty());

        error = table.add(HashMap.of(
                "id", DbType.from_raw(3),
                "foo", DbType.from_raw("extra text"),
                "bar", DbType.from_raw(420.0f),
                "spam", DbType.from_raw(true)
        ));
        assertTrue(error.isEmpty());
    }
}