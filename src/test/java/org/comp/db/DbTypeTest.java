package org.comp.db;

import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbTypeTest {

    @Test
    void from_primitive() {
        var dbInt = DbType.from_primitive(42);
        assertTrue(dbInt.isDefined());
        assertEquals(42, dbInt.get().get());

        var dbFloat = DbType.from_primitive(42.0f);
        assertTrue(dbFloat.isDefined());
        assertEquals(42.0f, dbFloat.get().get());

        var dbBoolean = DbType.from_primitive(false);
        assertTrue(dbBoolean.isDefined());
        assertFalse((boolean) dbBoolean.get().get());

        var dbString = DbType.from_primitive("this is text");
        assertTrue(dbString.isDefined());
        assertEquals("this is text", dbString.get().get());

        var fail0 = DbType.from_primitive(Option.none());
        assertTrue(fail0.isEmpty());

        int[] arr = {1, 2, 3, 4};
        var fail1 = DbType.from_primitive(arr);
        assertTrue(fail1.isEmpty());

        var fail3 = DbType.from_primitive(new Object());
        assertTrue(fail3.isEmpty());
    }

}