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
                "id", DbType.Integer.from(32).get(),
                "spam", DbType.Bool.from(false).get()
        );
        assertTrue(table.checkRecord(record0).isDefined());
        assertEquals(
                table.checkRecord(record0).get(),
                DbTable.RecordError.MissingField
        );

        // Extra fields
        HashMap<String, DbType> record1 = HashMap.of(
                "id", DbType.Integer.from(32).get(),
                "foo", DbType.String.from("this is text").get(),
                "bar", DbType.Float.from(42.0f).get(),
                "spam", DbType.Bool.from(false).get(),
                "extra", DbType.Integer.from(67).get()
        );
        assertTrue(table.checkRecord(record1).isDefined());
        assertEquals(
                table.checkRecord(record1).get(),
                DbTable.RecordError.ExtraFields
        );

        // Bad typing 1
        HashMap<String, DbType> record2 = HashMap.of(
                "id", DbType.Integer.from(32).get(),
                "foo", DbType.String.from("this is text").get(),
                "bar", DbType.Integer.from(42).get(),
                "spam", DbType.Bool.from(false).get()
        );
        assertTrue(table.checkRecord(record2).isDefined());
        assertEquals(
                table.checkRecord(record2).get(),
                DbTable.RecordError.BadTyping
        );

        // Bad typing 2
        HashMap<String, DbType> record3 = HashMap.of(
                "id", DbType.Integer.from(32).get(),
                "foo", DbType.Integer.from(42).get(),
                "bar", DbType.Float.from(42.0f).get(),
                "spam", DbType.Bool.from(false).get()
        );
        assertTrue(table.checkRecord(record3).isDefined());
        assertEquals(
                table.checkRecord(record3).get(),
                DbTable.RecordError.BadTyping
        );

        // Correct case
        HashMap<String, DbType> record4 = HashMap.of(
                "id", DbType.Integer.from(32).get(),
                "foo", DbType.String.from("this is text").get(),
                "bar", DbType.Float.from(42.0f).get(),
                "spam", DbType.Bool.from(false).get()
        );
        assertTrue(table.checkRecord(record4).isEmpty());
    }
}