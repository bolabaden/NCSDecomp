// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.Variable;
import com.kotor.resource.formats.ncs.utils.Type;

public class AVarDecl extends ScriptNode {
   private Variable var;
   private AExpression exp;
   private boolean isFcnReturn;

   public AVarDecl(Variable var) {
      this.var(var);
      this.exp = null;
      this.isFcnReturn = false;
   }

   public void var(Variable var) {
      this.var = var;
   }

   public Variable var() {
      return this.var;
   }

   public void isFcnReturn(boolean is) {
      this.isFcnReturn = is;
   }

   public boolean isFcnReturn() {
      return this.isFcnReturn;
   }

   public Type type() {
      return this.var.type();
   }

   public void initializeExp(AExpression exp) {
      exp.parent(this);
      this.exp = exp;
   }

   public AExpression removeExp() {
      AExpression aexp = this.exp;
      this.exp.parent(null);
      this.exp = null;
      return aexp;
   }

   public AExpression exp() {
      return this.exp;
   }

   @Override
   public String toString() {
      return this.exp == null
         ? this.tabs + this.var.toDeclString() + ";" + this.newline
         : this.tabs + this.var.toDeclString() + " = " + this.exp.toString() + ";" + this.newline;
   }

   @Override
   public void close() {
      super.close();
      if (this.exp != null) {
         ((ScriptNode)this.exp).close();
      }

      this.exp = null;
      if (this.var != null) {
         this.var.close();
      }

      this.var = null;
   }
}

