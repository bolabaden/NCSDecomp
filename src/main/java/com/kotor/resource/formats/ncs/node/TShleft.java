// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class TShleft extends Token {
   public TShleft() {
      super.setText("SHLEFTII");
   }

   public TShleft(int line, int pos) {
      super.setText("SHLEFTII");
      this.setLine(line);
      this.setPos(pos);
   }

   @Override
   public Object clone() {
      return new TShleft(this.getLine(), this.getPos());
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseTShleft(this);
   }

   @Override
   public void setText(String text) {
      throw new RuntimeException("Cannot change TShleft text.");
   }
}

