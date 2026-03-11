package org.comp.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbManagerTest {

    @Test
    void getActiveDatabase() {
        DbManager.reset();
        DbManager mng = DbManager.getInstance();
        assertTrue(mng.getActiveDatabase().isEmpty());

        var db = mng.createDatabase("db0", "sample.json").getLeft();
        var otherDb = mng.setActiveDatabase("db0").getLeft();

        assertEquals(db, otherDb);
    }

    @Test
    void setActiveDatabase() {
        DbManager.reset();
        DbManager mng = DbManager.getInstance();
        var db = mng.createDatabase("db0", "sample.json", true).getLeft();
        var otherDb = mng.getActiveDatabase().get();

        assertEquals(db, otherDb);
    }

    @Test
    void createDatabase() {
        DbManager.reset();
        DbManager mng0 = DbManager.getInstance();
        DbManager mng1 = DbManager.getInstance();
        assertEquals(mng0, mng1);

        // Creating DBs
        var maybeErr = mng0.createDatabase("db0", "sample.json");
        assertTrue(maybeErr.isLeft());

        maybeErr = mng1.createDatabase("db0", "sample.json");
        assertTrue(maybeErr.isRight());
        assertEquals(maybeErr.get(), DbManager.DatabaseError.ExistingDb);
    }
}