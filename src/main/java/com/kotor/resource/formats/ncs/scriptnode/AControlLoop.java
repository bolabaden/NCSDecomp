// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public class AControlLoop extends ScriptRootNode {
   protected AExpression condition;

   public AControlLoop(int start, int end) {
      super(start, end);
   }

   public void end(int end) {
      this.end = end;
   }

   public void condition(AExpression condition) {
      condition.parent(this);
      this.condition = condition;
   }

   public AExpression condition() {
      return this.condition;
   }

   /**
    * Returns the condition wrapped in a single pair of parentheses, adding them only when needed.
    */
   protected String formattedCondition() {
      if (this.condition == null) {
         return " ()";
      }

      String cond = this.condition.toString().trim();
      boolean wrapped = cond.startsWith("(") && cond.endsWith(")");
      String wrappedCond = wrapped ? cond : "(" + cond + ")";
      return " " + wrappedCond;
   }

   @Override
   public void close() {
      super.close();
      if (this.condition != null) {
         ((ScriptNode)this.condition).close();
         this.condition = null;
      }
   }
}

