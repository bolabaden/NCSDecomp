// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class TNequal extends Token {
   public TNequal() {
      super.setText("NEQUAL");
   }

   public TNequal(int line, int pos) {
      super.setText("NEQUAL");
      this.setLine(line);
      this.setPos(pos);
   }

   @Override
   public Object clone() {
      return new TNequal(this.getLine(), this.getPos());
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseTNequal(this);
   }

   @Override
   public void setText(String text) {
      throw new RuntimeException("Cannot change TNequal text.");
   }
}

