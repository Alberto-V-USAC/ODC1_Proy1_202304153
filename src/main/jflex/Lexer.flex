package org.comp;

import java_cup.runtime.Symbol;

%%

%public
%class Lexer
%unicode
%cup
%line
%column

%{
    // Initialize StringBuilder in order to read strings
    StringBuilder string = new StringBuilder();

    private Symbol symbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn + 1);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }

%}

// ========== Macro rules ==========
// Basic utils
LineTerminator  = \r|\n|\r\n
InputCharacter  = [^\r\n]
WhiteSpace      = {LineTerminator} | [ \t\f]
Identifier      = [:jletter:][:jletterdigit:]*

// Comments
InlineComment   = "##" {InputCharacter}* {LineTerminator}
BlockComment    = "#*" ~ "*#"

// Literals
IntegerLit  = 0 | ([+-]? [1-9][0-9]*)
FloatLit    = {FLit1} | {FLit2} | {IntegerLit}

FLit1   = [+-]? [0-9]+ \. [0-9]*
FLit2   = [+-]? \. [0-9]*

StringCharacter = [^\r\n\"\\]


%state STRING

%%
// ========== Lexical rules ==========
<YYINITIAL> {
    // Keywords
    "database"      { return symbol(sym.DATABASE); }
    "export"        { return symbol(sym.EXPORT); }
    "table"         { return symbol(sym.TABLE); }
    "read"          { return symbol(sym.READ); }
    "add"           { return symbol(sym.ADD); }
    "update"        { return symbol(sym.UPDATE); }
    "clear"         { return symbol(sym.CLEAR); }
    "int"           { return symbol(sym.INT_T); }
    "float"         { return symbol(sym.FLOAT_T); }
    "bool"          { return symbol(sym.BOOL_T); }
    "string"        { return symbol(sym.STRING_T); }
    "array"         { return symbol(sym.ARRAY_T); }
    "object"        { return symbol(sym.OBJECT_T); }
    "null"          { return symbol(sym.NullLit); }
    "use"           { return symbol(sym.USE); }
    "store at"      { return symbol(sym.STORE_AT); }
    "set"           { return symbol(sym.SET); }
    "fields"        { return symbol(sym.FIELDS); }
    "filter"        { return symbol(sym.FILTER); }
    "star"          { return symbol(sym.STAR); }

    // Operators
    "!"     { return symbol(sym.NOT); }
    "=="    { return symbol(sym.EQEQ); }
    "!="    { return symbol(sym.NOTEQ); }
    ">"     { return symbol(sym.GT); }
    "<"     { return symbol(sym.LT); }
    "<="    { return symbol(sym.LEQ); }
    ">="    { return symbol(sym.GEQ); }
    "&&"    { return symbol(sym.ANDAND); }
    "||"    { return symbol(sym.OROR); }

    // Separators
    {WhiteSpace}    { }
    {InlineComment}  { /* Ignore comments */ }
    {BlockComment}  { /* Ignore comments */ }

    "("    { return symbol(sym.LPAREN); }
    ")"    { return symbol(sym.RPAREN); }
    "{"    { return symbol(sym.LBRACE); }
    "}"    { return symbol(sym.RBRACE); }
    "["    { return symbol(sym.LBRACK); }
    "]"    { return symbol(sym.RBRACK); }
    ";"    { return symbol(sym.SEMICOLON); }
    ":"    { return symbol(sym.COLON); }
    ","    { return symbol(sym.COMMA); }

    // Boolean Literals
    "true"   { return symbol(sym.BoolLit, true); }
    "false"  { return symbol(sym.BoolLit, false); }

    // Numerical Literals
    {IntegerLit}    { return symbol(sym.IntegerLit, Integer.parseInt(yytext())); }
    {FloatLit}    { return symbol(sym.FloatLit, Float.parseFloat(yytext())); }

    {Identifier}    { return symbol(sym.Identifier, yytext()); }

    // String literal
    \" {
          string.setLength(0);  // Clear buffer
          yybegin(STRING);      // Switch to STRING state
      }
}

<STRING> {
    \" {
          yybegin(YYINITIAL);
          return symbol(sym.StringLit, string.toString());
      }

    {StringCharacter}+  { string.append(yytext()); }

    // Escape sequences
    "\\b"                          { string.append('\b'); }
    "\\t"                          { string.append('\t'); }
    "\\n"                          { string.append('\n'); }
    "\\f"                          { string.append('\f'); }
    "\\r"                          { string.append('\r'); }
    "\\\""                         { string.append('\"'); }
    "\\'"                          { string.append('\''); }
    "\\\\"                         { string.append('\\'); }

    // Error cases
    \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
    {LineTerminator}               { throw new RuntimeException("Unterminated string at end of line"); }
}

<<EOF>>          { return symbol(sym.EOF); }