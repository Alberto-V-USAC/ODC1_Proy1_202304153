package org.comp.db;

import io.vavr.collection.List;

public class Database {
    String storePath;
    List<DbTable> tables = List.empty();
}
