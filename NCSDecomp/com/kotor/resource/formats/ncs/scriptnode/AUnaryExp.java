// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;

public class AUnaryExp extends ScriptNode implements AExpression {
   private AExpression exp;
   private String op;
   private StackEntry stackentry;

   public AUnaryExp(AExpression exp, String op) {
      this.exp(exp);
      this.op = op;
   }

   protected void exp(AExpression exp) {
      this.exp = exp;
      exp.parent(this);
   }

   @Override
   public String toString() {
      return "(" + this.op + this.exp.toString() + ")";
   }

   @Override
   public StackEntry stackentry() {
      return this.stackentry;
   }

   @Override
   public void stackentry(StackEntry stackentry) {
      this.stackentry = stackentry;
   }

   @Override
   public void close() {
      super.close();
      if (this.exp != null) {
         ((ScriptNode)this.exp).close();
      }

      this.exp = null;
      if (this.stackentry != null) {
         this.stackentry.close();
      }

      this.stackentry = null;
   }
}

