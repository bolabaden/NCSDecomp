// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.Const;
import com.kotor.resource.formats.ncs.stack.StackEntry;

public class AConst extends ScriptNode implements AExpression {
   private Const theconst;

   public AConst(Const theconst) {
      this.theconst = theconst;
   }

   @Override
   public String toString() {
      return this.theconst.toString();
   }

   @Override
   public StackEntry stackentry() {
      return this.theconst;
   }

   @Override
   public void stackentry(StackEntry stackentry) {
      this.theconst = (Const)stackentry;
   }

   @Override
   public void close() {
      super.close();
      this.theconst = null;
   }
}

