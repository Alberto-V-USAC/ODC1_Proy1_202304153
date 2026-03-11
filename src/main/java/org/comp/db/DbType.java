package org.comp.db;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;

public sealed interface DbType permits
        DbType.Integer,
        DbType.Float,
        DbType.Bool,
        DbType.String,
        DbType.Array,
        DbType.DbObject
{
    final class Integer implements DbType {
        int inner;
    }

    final class Float implements DbType {
        float inner;
    }

    final class Bool implements DbType {
        boolean inner;
    }

    final class String implements DbType {
        String inner;
    }

    final class DbObject implements DbType {
        HashMap<String, DbType> inner = HashMap.empty();
    }

    final class Array implements DbType {
        List<DbType> inner = List.of();
    }
}
