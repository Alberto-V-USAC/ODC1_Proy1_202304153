package org.comp;

import java_cup.runtime.Symbol;

%%

%class Lexer
%unicode
%cup
%line
%column

%{

private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
}

private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
}

%}

NUMBER = [0-9]+
WHITESPACE = [ \t\r\n]+

%%

{WHITESPACE}     { }

"+"              { return symbol(sym.PLUS); }
"-"              { return symbol(sym.MINUS); }
"*"              { return symbol(sym.TIMES); }
"/"              { return symbol(sym.DIVIDE); }

"("              { return symbol(sym.LPAREN); }
")"              { return symbol(sym.RPAREN); }

{NUMBER}         { return symbol(sym.NUMBER, Integer.parseInt(yytext())); }

<<EOF>>          { return symbol(sym.EOF); }