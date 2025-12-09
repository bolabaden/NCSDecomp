// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

/**
 * Terminal token for the ADD opcode emitted by the lexer.
 */
public final class TAdd extends Token {
   public TAdd() {
      super.setText("ADD");
   }

   public TAdd(int line, int pos) {
      super.setText("ADD");
      this.setLine(line);
      this.setPos(pos);
   }

   @Override
   public Object clone() {
      return new TAdd(this.getLine(), this.getPos());
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseTAdd(this);
   }

   @Override
   public void setText(String text) {
      throw new RuntimeException("Cannot change TAdd text.");
   }
}

