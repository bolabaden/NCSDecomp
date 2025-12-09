// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class TNop extends Token {
   public TNop() {
      super.setText("NOP");
   }

   public TNop(int line, int pos) {
      super.setText("NOP");
      this.setLine(line);
      this.setPos(pos);
   }

   @Override
   public Object clone() {
      return new TNop(this.getLine(), this.getPos());
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseTNop(this);
   }

   @Override
   public void setText(String text) {
      throw new RuntimeException("Cannot change TNop text.");
   }
}

