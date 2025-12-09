// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public class AReturnStatement extends ScriptNode {
   protected AExpression returnexp;

   public AReturnStatement() {
   }

   public AReturnStatement(AExpression returnexp) {
      this.returnexp(returnexp);
   }

   public void returnexp(AExpression returnexp) {
      returnexp.parent(this);
      this.returnexp = returnexp;
   }

   public AExpression exp() {
      return this.returnexp;
   }

   @Override
   public String toString() {
      return this.returnexp == null ? this.tabs + "return;" + this.newline : this.tabs + "return " + this.returnexp.toString() + ";" + this.newline;
   }

   @Override
   public void close() {
      super.close();
      if (this.returnexp != null) {
         ((ScriptNode)this.returnexp).close();
      }

      this.returnexp = null;
   }
}

