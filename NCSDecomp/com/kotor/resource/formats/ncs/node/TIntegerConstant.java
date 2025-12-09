// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

/**
 * Terminal token for integer literals (captures text and source position).
 */
public final class TIntegerConstant extends Token {
   public TIntegerConstant(String text) {
      this.setText(text);
   }

   public TIntegerConstant(String text, int line, int pos) {
      this.setText(text);
      this.setLine(line);
      this.setPos(pos);
   }

   @Override
   public Object clone() {
      return new TIntegerConstant(this.getText(), this.getLine(), this.getPos());
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseTIntegerConstant(this);
   }
}

