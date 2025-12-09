// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class EOF extends Token {
   public EOF() {
      this.setText("");
   }

   public EOF(int line, int pos) {
      this.setText("");
      this.setLine(line);
      this.setPos(pos);
   }

   @Override
   public Object clone() {
      return new EOF(this.getLine(), this.getPos());
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseEOF(this);
   }
}

