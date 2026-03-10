package org.comp;

%%

%class Lexer
%public
%unicode
%line
%column
%type String

DIGIT = [0-9]
ID    = [a-zA-Z][a-zA-Z0-9]*

%%

"+"        { return "PLUS"; }
"-"        { return "MINUS"; }
"*"        { return "MULT"; }
"/"        { return "DIV"; }

{DIGIT}+   { return "INT(" + yytext() + ")"; }

{ID}       { return "ID(" + yytext() + ")"; }

[ \t\r\n]+ { /* skip whitespace */ }

.          { return "UNKNOWN(" + yytext() + ")"; }