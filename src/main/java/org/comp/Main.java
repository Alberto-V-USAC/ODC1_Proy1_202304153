package org.comp;

import java.io.StringReader;

public class Main {
    public static void main(String[] args) throws Exception {
        String input = "sum = a1 + 23 * value";

        Lexer lexer = new Lexer(new StringReader(input));

        String token;
        while ((token = lexer.yylex()) != null) {
            System.out.println(token);
        }
    }
}