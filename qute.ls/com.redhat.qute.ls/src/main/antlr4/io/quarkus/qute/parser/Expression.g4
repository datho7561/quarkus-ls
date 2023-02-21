/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/

/**
 * Defines the parser and lexer for https://quarkus.io/guides/qute-reference#expressions
 */
grammar Expression;

/*
 * Parser rules
 */

expression: literal;

literal: numberLiteral | booleanLiteral | stringLiteral | voidLiteral;
numberLiteral: NUMBER_LITERAL;
booleanLiteral: BOOLEAN_LITERAL;
stringLiteral: SINGLE_QUOTE_STRING | DOUBLE_QUOTE_STRING;
voidLiteral: NULL;

/*
 * Lexer Rules
 */

WHITESPACE: ' ' -> skip;
OPEN_PAREN: '(';
CLOSE_PAREN: ')';
OPEN_SQUARE_BRACE: '[';
CLOSE_SQUARE_BRACE: ']';
COLON: ':';
QUESTION_MARK: '?';

NUMBER_LITERAL: [0-9]+;

/* operators */
ELVIS_OPERATOR: '?:';
OR_OPERATOR: '||';
AND_OPERATOR: '&&';
PLUS_OPERATOR: '+';
MINUS_OPERATOR: '-';

SINGLE_QUOTE_STRING: '\'' [^']+ '\'';
DOUBLE_QUOTE_STRING: '\'' [^']+ '\'';

BOOLEAN_LITERAL: 'true' | 'false';
NULL: 'null';