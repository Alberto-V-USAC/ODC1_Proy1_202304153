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
        DbTable table = new DbTable("table", basicSchema);

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

    @Test
    void read() {
        DbTable table = new DbTable("table", basicSchema);

        table.add(HashMap.of(
                "id", DbType.from_raw(1),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false)
        ));

        table.add(HashMap.of(
                "id", DbType.from_raw(2),
                "foo", DbType.from_raw("this is more text"),
                "bar", DbType.from_raw(13.0f),
                "spam", DbType.from_raw(false)
        ));

        table.add(HashMap.of(
                "id", DbType.from_raw(3),
                "foo", DbType.from_raw("extra text"),
                "bar", DbType.from_raw(420.0f),
                "spam", DbType.from_raw(true)
        ));

        // Assert size
        assertEquals(table.read().size(), 3);

        // Several filters
        var filtered = table.read(map -> !((boolean) map.get("spam").get().get()));
        assertEquals(filtered.size(), 2);

        filtered = table.read(map -> (int) map.get("id").get().get() == 2);
        assertEquals(filtered.size(), 1);

        filtered = table.read(map -> (int) map.get("id").get().get() == 1);
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.head().get("foo").get().get(), "this is text");

        filtered = table.read(map -> (float) map.get("bar").get().get() == 420.0f);
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.head().get("foo").get().get(), "extra text");
    }

    @Test
    void update() {
        DbTable table = new DbTable("table", basicSchema);

        table.add(HashMap.of(
                "id", DbType.from_raw(1),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false)
        ));

        table.add(HashMap.of(
                "id", DbType.from_raw(2),
                "foo", DbType.from_raw("this is more text"),
                "bar", DbType.from_raw(13.0f),
                "spam", DbType.from_raw(false)
        ));

        table.add(HashMap.of(
                "id", DbType.from_raw(3),
                "foo", DbType.from_raw("extra text"),
                "bar", DbType.from_raw(420.0f),
                "spam", DbType.from_raw(true)
        ));

        // Update and assert
        table.update(
                HashMap.of("foo", DbType.from_raw("this is an updated text")),
                rec -> (int) rec.get("id").get().get() == 1
        );
        var text0 = (String) table
                .read(rec -> (int) rec.get("id").get().get() == 1)
                .head()
                .get("foo").get().get();
        assertEquals("this is an updated text", text0);

        // Update again and assert
        table.update(
                HashMap.of(
                        "foo", DbType.from_raw("Moar text"),
                        "bar", DbType.from_raw(10.0f)
                ),
                rec -> (int) rec.get("id").get().get() == 2
        );
        var filteredRecord = table
                .read(rec -> (int) rec.get("id").get().get() == 2)
                .head();
        var text1 = (String) filteredRecord.get("foo").get().get();
        var text2 = (float) filteredRecord.get("bar").get().get();

        assertEquals("Moar text", text1);
        assertEquals(10.0f, text2);
    }

    @Test
    void clear() {
        DbTable table = new DbTable("table", basicSchema);

        table.add(HashMap.of(
                "id", DbType.from_raw(1),
                "foo", DbType.from_raw("this is text"),
                "bar", DbType.from_raw(42.0f),
                "spam", DbType.from_raw(false)
        ));

        table.add(HashMap.of(
                "id", DbType.from_raw(2),
                "foo", DbType.from_raw("this is more text"),
                "bar", DbType.from_raw(13.0f),
                "spam", DbType.from_raw(false)
        ));

        table.add(HashMap.of(
                "id", DbType.from_raw(3),
                "foo", DbType.from_raw("extra text"),
                "bar", DbType.from_raw(420.0f),
                "spam", DbType.from_raw(true)
        ));

        table.clear();
        assertTrue(table.read().isEmpty());
    }
}