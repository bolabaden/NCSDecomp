// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.
package com.kotor.resource.formats.ncs.scriptnode;

/**
 * Centralized expression pretty-printer that minimizes redundant parentheses
 * while keeping operator precedence and associativity intact. Expressions render
 * through this formatter instead of hand-built toString implementations in
 * individual nodes so nested expressions share consistent formatting rules.
 */
final class ExpressionFormatter {
   private enum Position {
      NONE,
      LEFT,
      RIGHT;
   }

   private static final int PREC_ASSIGNMENT = 1;
   private static final int PREC_LOGICAL_OR = 2;
   private static final int PREC_LOGICAL_AND = 3;
   private static final int PREC_BIT_OR = 4;
   private static final int PREC_BIT_XOR = 5;
   private static final int PREC_BIT_AND = 6;
   private static final int PREC_EQUALITY = 7;
   private static final int PREC_RELATIONAL = 8;
   private static final int PREC_SHIFT = 9;
   private static final int PREC_ADDITIVE = 10;
   private static final int PREC_MULTIPLICATIVE = 11;
   private static final int PREC_UNARY = 12;

   private ExpressionFormatter() {
   }

   static String format(AExpression expr) {
      return format(expr, Integer.MAX_VALUE, Position.NONE, null);
   }

   /**
    * Formats an expression for value contexts (variable initializers, returns)
    * and preserves explicit grouping for simple comparison operations to match
    * the original source style used by most shipped scripts.
    */
   static String formatValue(AExpression expr) {
      String rendered = format(expr);
      if (expr instanceof ABinaryExp && needsValueParens((ABinaryExp) expr)) {
         return ensureWrapped(rendered);
      }
      return rendered;
   }

   private static String format(AExpression expr, int parentPrec, Position side, String parentOp) {
      if (expr == null) {
         return "";
      }

      if (expr instanceof ABinaryExp) {
         return formatBinary((ABinaryExp) expr, parentPrec, side, parentOp);
      }
      if (expr instanceof AConditionalExp) {
         return formatConditional((AConditionalExp) expr, parentPrec, side, parentOp);
      }
      if (expr instanceof AUnaryExp) {
         return formatUnary((AUnaryExp) expr, parentPrec, side, parentOp);
      }
      if (expr instanceof AUnaryModExp) {
         return formatUnaryMod((AUnaryModExp) expr, parentPrec, side, parentOp);
      }
      if (expr instanceof AModifyExp) {
         return formatAssignment((AModifyExp) expr, parentPrec, side, parentOp);
      }

      // Leaf-ish nodes keep their own rendering (constants, function calls, etc.)
      return expr.toString();
   }

   private static String formatBinary(ABinaryExp exp, int parentPrec, Position side, String parentOp) {
      String op = exp.op();
      int prec = precedence(op);
      String left = format(exp.left(), prec, Position.LEFT, op);
      String right = format(exp.right(), prec, Position.RIGHT, op);
      String rendered = left + " " + op + " " + right;
      return wrapIfNeeded(rendered, prec, parentPrec, side, parentOp, op);
   }

   private static String formatConditional(AConditionalExp exp, int parentPrec, Position side, String parentOp) {
      String op = exp.op();
      int prec = precedence(op);
      String left = format(exp.left(), prec, Position.LEFT, op);
      String right = format(exp.right(), prec, Position.RIGHT, op);
      String rendered = left + " " + op + " " + right;
      return wrapIfNeeded(rendered, prec, parentPrec, side, parentOp, op);
   }

   private static String formatUnary(AUnaryExp exp, int parentPrec, Position side, String parentOp) {
      String op = exp.op();
      int prec = PREC_UNARY;
      String inner = format(exp.exp(), prec, Position.RIGHT, op);
      String rendered = op + inner;
      return wrapIfNeeded(rendered, prec, parentPrec, side, parentOp, op);
   }

   private static String formatUnaryMod(AUnaryModExp exp, int parentPrec, Position side, String parentOp) {
      String op = exp.op();
      int prec = PREC_UNARY;
      String target = format(exp.varRef(), prec, Position.RIGHT, op);
      String rendered = exp.prefix() ? op + target : target + op;
      return wrapIfNeeded(rendered, prec, parentPrec, side, parentOp, op);
   }

   private static String formatAssignment(AModifyExp exp, int parentPrec, Position side, String parentOp) {
      int prec = PREC_ASSIGNMENT;
      String left = format(exp.varRef(), prec, Position.LEFT, "=");
      String right = format(exp.expression(), prec, Position.RIGHT, "=");
      String rendered = left + " = " + right;
      return wrapIfNeeded(rendered, prec, parentPrec, side, parentOp, "=");
   }

   private static String wrapIfNeeded(String rendered, int selfPrec, int parentPrec, Position side, String parentOp,
         String selfOp) {
      return shouldParenthesize(selfPrec, parentPrec, side, parentOp, selfOp) ? "(" + rendered + ")" : rendered;
   }

   private static boolean shouldParenthesize(int selfPrec, int parentPrec, Position side, String parentOp,
         String selfOp) {
      if (parentPrec == Integer.MAX_VALUE) {
         return false; // top-level expressions never need wrapping
      }
      if (selfPrec < parentPrec) {
         return true; // child binds looser than parent
      }
      if (selfPrec > parentPrec || parentOp == null) {
         return false;
      }

      // Equal precedence: parenthesize right-hand children when associativity differs
      if (side == Position.RIGHT) {
         if (isNonAssociative(parentOp)) {
            return true;
         }
         return !parentOp.equals(selfOp);
      }

      return false;
   }

   private static int precedence(String op) {
      if (op == null) {
         return PREC_UNARY; // safest default for unknown operators
      }
      switch (op) {
         case "||":
            return PREC_LOGICAL_OR;
         case "&&":
            return PREC_LOGICAL_AND;
         case "|":
            return PREC_BIT_OR;
         case "^":
            return PREC_BIT_XOR;
         case "&":
            return PREC_BIT_AND;
         case "==":
         case "!=":
            return PREC_EQUALITY;
         case "<":
         case "<=":
         case ">":
         case ">=":
            return PREC_RELATIONAL;
         case "<<":
         case ">>":
            return PREC_SHIFT;
         case "+":
         case "-":
            return PREC_ADDITIVE;
         case "*":
         case "/":
         case "%":
            return PREC_MULTIPLICATIVE;
         default:
            return PREC_UNARY;
      }
   }

   private static boolean isNonAssociative(String op) {
      if (op == null) {
         return false;
      }
      switch (op) {
         case "=":
         case "-":
         case "/":
         case "%":
         case "<<":
         case ">>":
         case "==":
         case "!=":
         case "<":
         case "<=":
         case ">":
         case ">=":
            return true;
         default:
            return false;
      }
   }

   private static boolean needsValueParens(ABinaryExp exp) {
      String op = exp.op();
      return isComparisonOp(op);
   }

   private static boolean isComparisonOp(String op) {
      if (op == null) {
         return false;
      }
      switch (op) {
         case "==":
         case "!=":
         case "<":
         case "<=":
         case ">":
         case ">=":
            return true;
         default:
            return false;
      }
   }

   private static String ensureWrapped(String rendered) {
      String trimmed = rendered.trim();
      return trimmed.startsWith("(") && trimmed.endsWith(")") ? rendered : "(" + rendered + ")";
   }
}

