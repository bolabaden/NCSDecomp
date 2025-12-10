// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.analysis.PrunedReversedDepthFirstAdapter;
import com.kotor.resource.formats.ncs.node.Node;

/**
 * Assigns bytecode positions to AST nodes for downstream analyses.
 */
public class SetPositions extends PrunedReversedDepthFirstAdapter {
   private NodeAnalysisData nodedata;
   private int currentPos;

   public SetPositions(NodeAnalysisData nodedata) {
      this.nodedata = nodedata;
      this.currentPos = 0;
   }

   public void done() {
      this.nodedata = null;
   }

   @Override
   public void defaultIn(Node node) {
      int pos = NodeUtils.getCommandPos(node);
      if (pos > 0) {
         this.currentPos = pos;
      }
   }

   @Override
   public void defaultOut(Node node) {
      this.nodedata.setPos(node, this.currentPos);
   }
}

