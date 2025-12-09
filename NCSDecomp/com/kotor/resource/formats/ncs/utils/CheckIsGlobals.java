// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.analysis.PrunedReversedDepthFirstAdapter;
import com.kotor.resource.formats.ncs.node.ABpCommand;
import com.kotor.resource.formats.ncs.node.ACommandBlock;
import com.kotor.resource.formats.ncs.node.PCmd;

/**
 * Detects whether a subroutine represents the globals block (presence of BP ops).
 */
public class CheckIsGlobals extends PrunedReversedDepthFirstAdapter {
   private boolean isGlobals = false;

   @Override
   public void inABpCommand(ABpCommand node) {
      this.isGlobals = true;
   }

   @Override
   public void caseACommandBlock(ACommandBlock node) {
      this.inACommandBlock(node);
      Object[] temp = node.getCmd().toArray();

      for (int i = temp.length - 1; i >= 0; i--) {
         ((PCmd)temp[i]).apply(this);
         if (this.isGlobals) {
            return;
         }
      }

      this.outACommandBlock(node);
   }

   public boolean getIsGlobals() {
      return this.isGlobals;
   }
}

