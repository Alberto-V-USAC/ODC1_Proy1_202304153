package org.comp;

import org.comp.db.DbManager;
import ui.MainUI;

public class Main {

    public static void main(String[] args) throws Exception {
        String input = """
            ## This is a comment
            database db0 {
                store at "sample.json";
            }

            table table0 {
                val : int;
                foo : float;
                bar : string;
                baz : bool;
            }

            table table1 {
                yipi: array;
                nothing: object;
            }

            add table0 {
                val: 1,
                baz: true,
                foo: 12.3,
                bar: "hi"
            }

            add table0 {
                val: 2,
                baz: false,
                foo: 69.0,
                bar: "hello world"      ## cool thing with comments
            }

            add table0 {
                val: 3,
                baz: false,
                foo: 53.121,
                bar: "this is some text"
            }

            add table1 {
                yipi: [ 23, "yes", true, 34.2, [ "i'm", 14, "and", "this", "is", "deep" ] ],
                nothing: {
                    nombre: "juan",
                    edad: 69,
                    sexo: true
                }
            }
        """;

        MainUI ui = new MainUI();
    }
}
