// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;

public class AModifyExp extends ScriptNode implements AExpression {
   private AVarRef varref;
   private AExpression exp;

   public AModifyExp(AVarRef varref, AExpression exp) {
      this.varRef(varref);
      this.expression(exp);
   }

   protected void varRef(AVarRef varref) {
      this.varref = varref;
      varref.parent(this);
   }

   protected void expression(AExpression exp) {
      this.exp = exp;
      exp.parent(this);
   }

   public AExpression expression() {
      return this.exp;
   }

   public AVarRef varRef() {
      return this.varref;
   }

   @Override
   public String toString() {
      return this.varref.toString() + " = " + this.exp.toString();
   }

   @Override
   public StackEntry stackentry() {
      return this.varref.var();
   }

   @Override
   public void stackentry(StackEntry stackentry) {
   }

   @Override
   public void close() {
      super.close();
      if (this.exp != null) {
         ((ScriptNode)this.exp).close();
      }

      this.exp = null;
      if (this.varref != null) {
         this.varref.close();
      }

      this.varref = null;
   }
}

