// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;
import com.kotor.resource.formats.ncs.stack.VarStruct;
import com.kotor.resource.formats.ncs.stack.Variable;
import com.kotor.resource.formats.ncs.utils.Type;

public class AVarRef extends ScriptNode implements AExpression {
   private Variable var;

   public AVarRef(Variable var) {
      this.var(var);
   }

   public AVarRef(VarStruct struct) {
      this.var(struct);
   }

   public Type type() {
      return this.var.type();
   }

   public Variable var() {
      return this.var;
   }

   public void var(Variable var) {
      this.var = var;
   }

   public void chooseStructElement(Variable var) {
      if (VarStruct.class.isInstance(this.var) && ((VarStruct)this.var).contains(var)) {
         this.var = var;
      } else {
         throw new RuntimeException("Attempted to select a struct element not in struct");
      }
   }

   @Override
   public String toString() {
      return this.var.toString();
   }

   @Override
   public StackEntry stackentry() {
      return this.var;
   }

   @Override
   public void stackentry(StackEntry stackentry) {
      this.var((Variable)stackentry);
   }

   @Override
   public void close() {
      super.close();
      if (this.var != null) {
         this.var.close();
      }

      this.var = null;
   }
}

