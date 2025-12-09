// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;

public class AActionArgExp extends ScriptRootNode implements AExpression {
   public AActionArgExp(int start, int end) {
      super(start, end);
      this.start = start;
      this.end = end;
   }

   @Override
   public StackEntry stackentry() {
      return null;
   }

   @Override
   public void stackentry(StackEntry stackentry) {
   }
}

