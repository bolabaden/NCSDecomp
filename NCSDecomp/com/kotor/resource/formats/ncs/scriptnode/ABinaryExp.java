// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;

public class ABinaryExp extends ScriptNode implements AExpression {
   private AExpression left;
   private AExpression right;
   private String op;
   private StackEntry stackentry;

   public ABinaryExp(AExpression left, AExpression right, String op) {
      this.left(left);
      this.right(right);
      this.op = op;
   }

   protected void left(AExpression left) {
      this.left = left;
      left.parent(this);
   }

   protected void right(AExpression right) {
      this.right = right;
      right.parent(this);
   }

   @Override
   public String toString() {
      return "(" + this.left.toString() + " " + this.op + " " + this.right.toString() + ")";
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
      if (this.left != null) {
         ((ScriptNode)this.left).close();
         this.left = null;
      }

      if (this.right != null) {
         ((ScriptNode)this.right).close();
         this.right = null;
      }

      if (this.stackentry != null) {
         this.stackentry.close();
      }

      this.stackentry = null;
   }
}

