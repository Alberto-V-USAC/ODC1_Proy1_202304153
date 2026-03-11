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

        var dbFloat = DbType.from_primitive(42.0);
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

    @Test
    void from_array() {
        // Plain integer list
        java.lang.Integer[] intArr = {1, 2, 3, 4};
        var dbIntArr = DbType.from_array(intArr);

        assertTrue(dbIntArr.isDefined());
        assertArrayEquals(
                dbIntArr.get().get().map(v -> (int) v.get()).toJavaArray(),
                intArr
        );

        // Plain string list
        String[] strArr = { "this", "is", "text" };
        var dbStringArr = DbType.from_array(strArr);

        assertTrue(dbStringArr.isDefined());
        var strArray2 = dbStringArr
                .get()
                .get()
                .map(v -> (String) v.get())
                .toJavaList()
                .toArray(new String[0]);
        assertArrayEquals(strArr, strArray2);

        // Heterogeneous case
        Object[] objArr = { 42, "this is text", 6.7f, false };
        var dbObjArr = DbType.from_array(objArr);

        assertTrue(dbObjArr.isDefined());
        var objArr2 = dbObjArr
                .get()
                .get_raw()
                .toJavaArray();
        assertArrayEquals(objArr, objArr2);

        // Wrong case
        Object[] objArr3 = { Option.some(42), "this is text", 6.7, Option.none() };
        var dbObjArr3 = DbType.from_array(objArr3);
        assertTrue(dbObjArr3.isEmpty());
    }
}