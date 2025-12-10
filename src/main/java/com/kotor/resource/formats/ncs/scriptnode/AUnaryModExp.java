// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;

public class AUnaryModExp extends ScriptNode implements AExpression {
   private AVarRef varref;
   private String op;
   private boolean prefix;
   private StackEntry stackentry;

   public AUnaryModExp(AVarRef varref, String op, boolean prefix) {
      this.varRef(varref);
      this.op = op;
      this.prefix = prefix;
   }

   protected void varRef(AVarRef varref) {
      this.varref = varref;
      varref.parent(this);
   }

   @Override
   public String toString() {
      return this.prefix ? "(" + this.op + this.varref.toString() + ")" : "(" + this.varref.toString() + this.op + ")";
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
      if (this.varref != null) {
         this.varref.close();
      }

      this.varref = null;
      if (this.stackentry != null) {
         this.stackentry.close();
      }

      this.stackentry = null;
   }
}

