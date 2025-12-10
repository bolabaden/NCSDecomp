// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;

public class AVectorConstExp extends ScriptNode implements AExpression {
   private AExpression exp1;
   private AExpression exp2;
   private AExpression exp3;

   public AVectorConstExp(AExpression exp1, AExpression exp2, AExpression exp3) {
      this.exp1(exp1);
      this.exp2(exp2);
      this.exp3(exp3);
   }

   public void exp1(AExpression exp1) {
      this.exp1 = exp1;
      exp1.parent(this);
   }

   public void exp2(AExpression exp2) {
      this.exp2 = exp2;
      exp2.parent(this);
   }

   public void exp3(AExpression exp3) {
      this.exp3 = exp3;
      exp3.parent(this);
   }

   @Override
   public String toString() {
      return "[" + this.exp1 + "," + this.exp2 + "," + this.exp3 + "]";
   }

   @Override
   public StackEntry stackentry() {
      return null;
   }

   @Override
   public void stackentry(StackEntry stackentry) {
   }

   @Override
   public void close() {
      super.close();
      if (this.exp1 != null) {
         ((ScriptNode)this.exp1).close();
      }

      this.exp1 = null;
      if (this.exp2 != null) {
         ((ScriptNode)this.exp2).close();
      }

      this.exp2 = null;
      if (this.exp3 != null) {
         ((ScriptNode)this.exp3).close();
      }

      this.exp3 = null;
   }
}

