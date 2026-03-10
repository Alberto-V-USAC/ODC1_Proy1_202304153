package org.comp;

import java.io.StringReader;

public class Main {

    public static void main(String[] args) throws Exception {

        String input = "3 + 4 * (2 + 1)";

        Lexer lexer = new Lexer(new StringReader(input));
        Parser parser = new Parser(lexer);

        Object result = parser.parse().value;

        System.out.println("Result = " + result);
    }
}