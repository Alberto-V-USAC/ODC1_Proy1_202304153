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

    final class Integer implements DbType {
        int inner;

        @Override
        public java.lang.Integer get() {
            return inner;
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
        HashMap<String, DbType> inner = HashMap.empty();

        @Override
        public HashMap<String, DbType> get() {
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
