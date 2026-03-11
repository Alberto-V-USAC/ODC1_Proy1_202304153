package org.comp.db;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.comp.Lexer;
import org.comp.Parser;

import java.io.StringReader;

public class DbManager {
    public enum DatabaseError {
        MissingDb,
        ExistingDb
    }

    private static DbManager instance;
    private String log = "";
    private String latestLog = "";

    private List<Database> databases = List.empty();
    private Option<Database> activeDatabase = Option.none();

    private DbManager() {
    }

    public Option<Database> getActiveDatabase() {
        return activeDatabase;
    }

    public Either<Database, DatabaseError> setActiveDatabase(String dbName) {
        for (var db : databases) {
            if (db.dbName.equals(dbName)) {
                activeDatabase = Option.of(db);
                return Either.left(db);
            }
        }
        return Either.right(DatabaseError.MissingDb);
    }

    public Either<Database, DatabaseError> createDatabase(String dbName, String storePath) {
        return createDatabase(dbName, storePath, false);
    }

    public Either<Database, DatabaseError> createDatabase(String dbName, String storePath, boolean setActive) {
        if (databases.map(db -> db.dbName).contains(dbName))
            return Either.right(DatabaseError.ExistingDb);

        var db = new Database(dbName, storePath);

        if (setActive)
            activeDatabase = Option.of(db);

        databases = databases.push(db);
        return Either.left(db);
    }

    public void query(String query) throws Exception {
        Lexer lexer = new Lexer(new StringReader(query));
        Parser parser = new Parser(lexer);
        parser.parse();

        this.log += parser.LOG;
        this.latestLog = parser.latestLOG;
    }

    public void persist() {
        for (var db : databases) {
            db.persist();
        }
    }

    public String getLog() {
        return this.log;
    }

    public String getLatestLog() {
        return this.latestLog;
    }

    public static DbManager getInstance() {
        if (instance == null) {
            instance = new DbManager();
        }

        return instance;
    }

    public static void reset() {
        instance = null;
    }
}
