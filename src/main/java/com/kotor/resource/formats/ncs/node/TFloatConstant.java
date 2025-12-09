// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class TFloatConstant extends Token {
   public TFloatConstant(String text) {
      this.setText(text);
   }

   public TFloatConstant(String text, int line, int pos) {
      this.setText(text);
      this.setLine(line);
      this.setPos(pos);
   }

   @Override
   public Object clone() {
      return new TFloatConstant(this.getText(), this.getLine(), this.getPos());
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseTFloatConstant(this);
   }
}

