package org.comp.db;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Option;


public sealed interface DbType permits
        DbType.Integer,
        DbType.Float,
        DbType.Bool,
        DbType.String,
        DbType.Array,
        DbType.DbObject
{
    Object get();

    default java.lang.String format() {
        return switch (this) {
            case DbType.Integer dbType -> java.lang.String.valueOf(dbType.inner);
            case DbType.Float dbType -> java.lang.String.valueOf(dbType.inner);
            case DbType.Bool dbType -> java.lang.String.valueOf(dbType.inner);
            case DbType.String dbType -> dbType.inner;
            case DbType.Array arr -> {
                yield arr.inner
                        .map(DbType::format)
                        .mkString("[ ", ", ", " ]");
            }
            case DbType.DbObject obj -> {
                yield obj.inner
                        .map(entry -> entry._1() + " : " + entry._2().format())
                        .mkString("\n");
            }
        };
    }

    static Option<DbType> from_primitive(Object object) {
        return switch (object) {
            case java.lang.Integer ignored -> DbType.Integer.from(object).map(v -> v);
            case java.lang.Float ignored -> DbType.Float.from(object).map(v -> v);
            case java.lang.Double ignored -> DbType.Float.from(object).map(v -> v);
            case java.lang.Boolean ignored -> DbType.Bool.from(object).map(v -> v);
            case java.lang.String ignored -> DbType.String.from(object).map(v -> v);
            default -> Option.none();
        };
    }

    static Option<DbType.Array> from_array(Object[] arr) {
        var dbArray = new Array();

        for (Object obj : arr) {
            var maybeDbType = from_primitive(obj);
            if (maybeDbType.isEmpty())
                return Option.none();

            dbArray.inner = dbArray.inner.append(maybeDbType.get());
        }

        return Option.of(dbArray);
    }

    static Integer from_raw(int value) {
        return Integer.from_raw(value);
    }

    static Float from_raw(float value) {
        return Float.from_raw(value);
    }

    static Bool from_raw(boolean value) {
        return Bool.from_raw(value);
    }

    static String from_raw(java.lang.String value) {
        return String.from_raw(value);
    }

    static Array from_list(List<DbType> list) {
        Array array = new Array();
        array.inner = list;
        return array;
    }

    static DbObject from_map(HashMap<java.lang.String, DbType> map) {
        DbObject object = new DbObject();
        object.inner = map;
        return object;
    }

    final class Integer implements DbType {
        int inner;

        @Override
        public java.lang.Integer get() {
            return inner;
        }

        public static Integer from_raw(int value) {
            var dbType = new DbType.Integer();
            dbType.inner = value;
            return dbType;
        }

        public static Option<Integer> from(Object object) {
            if (object instanceof java.lang.Integer) {
                var dbType = new DbType.Integer();
                dbType.inner = (int) object;
                return Option.of(dbType);
            }
            return Option.none();
        }
    }

    final class Float implements DbType {
        float inner;

        @Override
        public java.lang.Float get() {
            return inner;
        }

        public static Float from_raw(float value) {
            var dbType = new DbType.Float();
            dbType.inner = value;
            return dbType;
        }

        public static Option<Float> from(Object object) {
            if (object instanceof java.lang.Float) {
                var dbType = new DbType.Float();
                dbType.inner = (float) object;
                return Option.of(dbType);
            }

            if (object instanceof java.lang.Double) {
                var dbType = new DbType.Float();
                dbType.inner = (float) (double) object;
                return Option.of(dbType);
            }

            return Option.none();
        }
    }

    final class Bool implements DbType {
        boolean inner;

        @Override
        public java.lang.Boolean get() {
            return inner;
        }

        public static Bool from_raw(boolean value) {
            var dbType = new DbType.Bool();
            dbType.inner = value;
            return dbType;
        }

        public static Option<Bool> from(Object object) {
            if (object instanceof java.lang.Boolean) {
                var dbType = new DbType.Bool();
                dbType.inner = (boolean) object;
                return Option.of(dbType);
            }
            return Option.none();
        }
    }

    final class String implements DbType {
        java.lang.String inner;

        @Override
        public java.lang.String get() {
            return inner;
        }

        public static String from_raw(java.lang.String value) {
            var dbType = new DbType.String();
            dbType.inner = value;
            return dbType;
        }

        public static Option<String> from(Object object) {
            if (object instanceof java.lang.String) {
                var dbType = new DbType.String();
                dbType.inner = (java.lang.String) object;
                return Option.of(dbType);
            }
            return Option.none();
        }
    }

    final class DbObject implements DbType {
        HashMap<java.lang.String, DbType> inner = HashMap.empty();

        @Override
        public HashMap<java.lang.String, DbType> get() {
            return inner;
        }
    }

    final class Array implements DbType {
        List<DbType> inner = List.of();

        @Override
        public List<DbType> get() {
            return inner;
        }

        public List<Object> get_raw() {
            return inner.map(DbType::get);
        }
    }
}
