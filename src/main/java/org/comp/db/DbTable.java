package org.comp.db;

import io.vavr.collection.HashMap;
import io.vavr.control.Option;

public class DbTable {
    String tableName;
    HashMap<String, Class<?>> schema = HashMap.empty();
    HashMap<String, Option<DbType>> registries = HashMap.empty();

    public DbTable(String tableName, HashMap<String, Class<?>> schema) {
        this.tableName = tableName;
        this.schema = schema;
    }
}
