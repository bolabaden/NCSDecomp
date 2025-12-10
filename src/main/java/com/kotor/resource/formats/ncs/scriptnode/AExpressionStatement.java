// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public class AExpressionStatement extends ScriptNode {
   private AExpression exp;

   public AExpressionStatement(AExpression exp) {
      exp.parent(this);
      this.exp = exp;
   }

   public AExpression exp() {
      return this.exp;
   }

   @Override
   public String toString() {
      return this.tabs + this.exp.toString() + ";" + this.newline;
   }

   @Override
   public void parent(ScriptNode parent) {
      super.parent(parent);
      this.exp.parent(this);
   }

   @Override
   public void close() {
      super.close();
      if (this.exp != null) {
         ((ScriptNode)this.exp).close();
      }

      this.exp = null;
   }
}

