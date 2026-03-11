package org.comp.db;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    HashMap<String, Class<?>> basicSchema0 = HashMap.of(
            "id", DbType.Integer.class,
            "foo", DbType.String.class,
            "bar", DbType.Float.class,
            "spam", DbType.Bool.class
    );

    HashMap<String, Class<?>> basicSchema1 = HashMap.of(
            "id", DbType.Integer.class,
            "spam", DbType.Bool.class,
            "eggs", DbType.Array.class
    );

    List<HashMap<String, DbType>> recordsSchema0 = List.of(
            createRecordSchema0(1, "Sample text", 1.0f, true),
            createRecordSchema0(2, "More text", 42.0f, false),
            createRecordSchema0(3, "Moar", 1.0f, false),
            createRecordSchema0(4, "Text", 32.2f, true),
            createRecordSchema0(5, "Thing", 1.0f, false)
    );
    List<HashMap<String, DbType>> recordsSchema1 = List.of(
            createRecordSchema1(1, true, new Integer[]{1, 2, 3}),
            createRecordSchema1(2, true, new Integer[]{4, 5, 6, 7}),
            createRecordSchema1(3, false, new Integer[]{8, 9}),
            createRecordSchema1(4, false, new Integer[]{})
    );

    static HashMap<String, DbType> createRecordSchema0(int a, String b, float c, boolean d) {
        return HashMap.of(
                "id", DbType.from_raw(a),
                "foo", DbType.from_raw(b),
                "bar", DbType.from_raw(c),
                "spam", DbType.from_raw(d)
        );
    }
    static HashMap<String, DbType> createRecordSchema1(int a, boolean b, Integer[] c) {
        return HashMap.of(
                "id", DbType.from_raw(a),
                "spam", DbType.from_raw(b),
                "eggs", DbType.from_array(c).get()
        );
    }

    @Test
    void addTable() {
        Database db = new Database("db0", "sample.json");

        // Bad table
        var err = db.addTable("badTable0", HashMap.of("foo", Integer.class));
        assertTrue(err.isDefined());
        assertEquals(err.get(), Database.TableError.BadSchema);

        // Ok table
        err = db.addTable("okTable", basicSchema0);
        assertTrue(err.isEmpty());

        // Repeated table
        err = db.addTable("okTable", HashMap.of("id", DbType.Integer.class));
        assertTrue(err.isDefined());
        assertEquals(err.get(), Database.TableError.ExistingTable);
    }

    @Test
    void readTable() {
        Database db = new Database("db0", "sample.json");
        db.addTable("table", basicSchema0);

        // Bad read
        var err = db.readTable("not existing table");
        assertTrue(err.isRight());
        assertEquals(err.get(), Database.TableError.TableNotFound);

        // Good read
        var maybeTable = db.readTable("table");
        assertTrue(maybeTable.isLeft());

        // Empty read
        var read = maybeTable.getLeft();
        assertTrue(read.isEmpty());

        // Enter some thingies
        db.addRecord("table", HashMap.of(
                "id", DbType.from_raw(1),
                "foo", DbType.from_raw("Some text"),
                "spam", DbType.from_raw(true)
        ));

        db.addRecord("table", HashMap.of(
                "id", DbType.from_raw(2),
                "foo", DbType.from_raw("More text"),
                "bar", DbType.from_raw(0.1f)
        ));

        read = db.readTable("table").getLeft();
        assertEquals(2, read.length());
    }

    @Test
    void testReadTable() {
        Database db = new Database("db0", "sample.json");
        db.addTable("table0", basicSchema0);
        db.addTable("table1", basicSchema1);

        for (var record : recordsSchema0) {
            db.addRecord("table0", record);
        }

        for (var record : recordsSchema1) {
            db.addRecord("table1", record);
        }

        var read = db.readTable("table0", record -> {
            int id = (int) record.get("id").get().get();
            return id == 1;
        }).getLeft();
        assertEquals(read.length(), 1);

        read = db.readTable("table0", record -> {
           return (boolean) record.get("spam").get().get();
        }).getLeft();
        assertEquals(read.length(), 2);

        read = db.readTable("table0", record -> {
            float num = (float) record.get("bar").get().get();
            return num == 1.0f;
        }).getLeft();
        assertEquals(read.length(), 3);

        read = db.readTable("table1", record -> {
            int id = (int) record.get("id").get().get();
            return id <= 3;
        }).getLeft();
        assertEquals(read.length(), 3);
    }

    @Test
    void readFilter() {
        Database db = new Database("db0", "sample.json");
        db.addTable("table0", basicSchema0);
        db.addTable("table1", basicSchema1);

        for (var record : recordsSchema0) {
            db.addRecord("table0", record);
        }

        for (var record : recordsSchema1) {
            db.addRecord("table1", record);
        }

        // Not existing table
        var maybeError = db.readFilter("not existing table");
        assertTrue(maybeError.isRight());
        assertEquals(maybeError.get(), Database.TableError.TableNotFound);

        // Cool things 0
        var filtered = db.readFilter("table0")
                .getLeft()
                .field("id")
                .filter(val -> (int) val.get() == 1)
                .collect();
        assertEquals(filtered.length(), 1);

        filtered = db.readFilter("table0")
                .getLeft()
                .field("id")
                .filter(val -> (int) val.get() <= 4)
                .field("spam")
                .filter(status -> (boolean) status.get())
                .collect();
        assertEquals(filtered.length(), 2);

        // Cool things 1
        filtered = db.readFilter("table1")
                .getLeft()
                .field("eggs")
                .filter(obj -> {
                    var arr = (List<DbType>) obj.get();
                    return arr.length() == 4;
                })
                .collect();

        var dbTypeArray = (List<DbType>) filtered.get().get("eggs").get().get();
        var intArr = dbTypeArray
                .map(dbtype -> (int) dbtype.get())
                .toJavaArray();

        assertArrayEquals(intArr, new Integer[]{4, 5, 6, 7});
    }

    @Test
    void addRecord() {
        Database db = new Database("db0", "sample.json");
        db.addTable("table0", basicSchema0);

        var err = db.addRecord("missing table", HashMap.of("example", DbType.from_raw(3)));
        assertEquals(err.get(), Database.AddError.TableNotFound);

        err = db.addRecord("table0",
                HashMap.of("id", DbType.from_raw(32.4f))
        );
        assertEquals(err.get(), Database.AddError.BadTyping);

        err = db.addRecord("table0",
                HashMap.of("not existing!!!", DbType.from_raw(2))
        );
        assertEquals(err.get(), Database.AddError.ExtraFields);

        err = db.addRecord("table0",
                HashMap.of("id", DbType.from_raw(2))
        );
        assertTrue(err.isEmpty());
    }

    @Test
    void addRecordExact() {
        Database db = new Database("db0", "sample.json");
        db.addTable("table0", basicSchema0);

        var err = db.addRecordExact("missing table", HashMap.of("example", DbType.from_raw(3)));
        assertEquals(err.get(), Database.AddError.TableNotFound);

        err = db.addRecordExact("table0",
                recordsSchema0.get(0).put("id", DbType.from_raw(32.0f))
        );
        assertEquals(err.get(), Database.AddError.BadTyping);

        err = db.addRecordExact("table0",
                HashMap.of("not existing!!!", DbType.from_raw(2))
        );
        assertEquals(err.get(), Database.AddError.ExtraFields);

        err = db.addRecordExact("table0",
                HashMap.of("id", DbType.from_raw(2))
        );
        assertEquals(err.get(), Database.AddError.IncompleteFields);

        err = db.addRecordExact("table0", recordsSchema0.get(0));
        assertTrue(err.isEmpty());
    }

    @Test
    void updateRecord() {
        Database db = new Database("db0", "sample.json");
        db.addTable("table0", basicSchema0);
        db.addTable("table1", basicSchema1);

        for (var record : recordsSchema0) {
            db.addRecord("table0", record);
        }

        for (var record : recordsSchema1) {
            db.addRecord("table1", record);
        }

        var err = db.updateRecord("missing table", HashMap.of("example", DbType.from_raw(3)));
        assertEquals(err.get(), Database.UpdateError.TableNotFound);

        err = db.updateRecord("table0", HashMap.of("id", DbType.from_raw(32.0f)));
        assertEquals(err.get(), Database.UpdateError.BadTyping);

        err = db.updateRecord("table0", HashMap.of("not existing!!!", DbType.from_raw(2)));
        assertEquals(err.get(), Database.UpdateError.ExtraFields);

        // Good branch
        err = db.updateRecord("table0", HashMap.of("foo", DbType.from_raw("hi")));
        assertTrue(err.isEmpty());

        assertTrue(db.readTable("table0")
                .getLeft()
                .forAll(rec -> (String) rec.get("foo").get().get() == "hi")
        );
    }

    @Test
    void testUpdateRecord() {
        Database db = new Database("db0", "sample.json");
        db.addTable("table0", basicSchema0);
        db.addTable("table1", basicSchema1);

        for (var record : recordsSchema0) {
            db.addRecord("table0", record);
        }

        for (var record : recordsSchema1) {
            db.addRecord("table1", record);
        }

        db.updateRecord(
                "table0",
                HashMap.of("foo", DbType.from_raw("hi")),
                rec -> {
                    float val = (float) rec.get("bar").get().get();
                    return val == 1.0f;
                }
        );

        var filtered = db.readFilter("table0")
                .getLeft()
                .field("foo")
                .filter(val -> (String) val.get() == "hi")
                .collect();

        assertEquals(filtered.length(), 3);
    }


}