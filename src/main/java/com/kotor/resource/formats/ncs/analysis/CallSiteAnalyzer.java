// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.
package com.kotor.resource.formats.ncs.analysis;

import com.kotor.resource.formats.ncs.ActionsData;
import com.kotor.resource.formats.ncs.node.AActionCommand;
import com.kotor.resource.formats.ncs.node.ABinaryCommand;
import com.kotor.resource.formats.ncs.node.AConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.AConstCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopBpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopSpCommand;
import com.kotor.resource.formats.ncs.node.ADestructCommand;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.ALogiiCommand;
import com.kotor.resource.formats.ncs.node.AMoveSpCommand;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.utils.NodeAnalysisData;
import com.kotor.resource.formats.ncs.utils.NodeUtils;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import com.kotor.resource.formats.ncs.utils.SubroutineState;
import com.kotor.resource.formats.ncs.utils.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Lightweight stack simulator that estimates parameter counts for JSR targets
 * based solely on call-site stack effects. It is conservative (prefers larger
 * counts) and resilient to unknown prototypes so it can run before full typing.
 */
public class CallSiteAnalyzer extends PrunedDepthFirstAdapter {
   private final NodeAnalysisData nodedata;
   private final SubroutineAnalysisData subdata;
   private final ActionsData actions;
   private final Map<Integer, Integer> inferredParams = new HashMap<>();
   private boolean skipdeadcode;
   private int height;
   private int growth;
   private SubroutineState state;

   public CallSiteAnalyzer(NodeAnalysisData nodedata, SubroutineAnalysisData subdata, ActionsData actions) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.actions = actions;
   }

   /**
    * Runs the analysis across all known subroutines.
    *
    * @return map of destination bytecode offsets to inferred parameter counts
    */
   public Map<Integer, Integer> analyze() {
      Iterator<ASubroutine> subs = this.subdata.getSubroutines();

      while (subs.hasNext()) {
         this.analyzeSubroutine(subs.next());
      }

      return this.inferredParams;
   }

   private void analyzeSubroutine(ASubroutine sub) {
      this.state = this.subdata.getState(sub);
      this.height = this.initialHeight();
      this.growth = 0;
      this.skipdeadcode = false;
      sub.apply(this);
   }

   private int initialHeight() {
      int initial = 0;
      if (this.state != null) {
         if (!this.state.type().equals((byte)0)) {
            initial++;
         }

         initial += this.state.getParamCount();
      }

      return initial;
   }

   @Override
   public void defaultIn(Node node) {
      if (NodeUtils.isCommandNode(node)) {
         this.skipdeadcode = !this.nodedata.processCode(node);
      }
   }

   @Override
   public void outARsaddCommand(ARsaddCommand node) {
      if (!this.skipdeadcode) {
         this.push(1);
      }
   }

   @Override
   public void outAConstCommand(AConstCommand node) {
      if (!this.skipdeadcode) {
         this.push(1);
      }
   }

   @Override
   public void outACopyTopSpCommand(ACopyTopSpCommand node) {
      if (!this.skipdeadcode) {
         this.push(NodeUtils.stackSizeToPos(node.getSize()));
      }
   }

   @Override
   public void outACopyTopBpCommand(ACopyTopBpCommand node) {
      if (!this.skipdeadcode) {
         this.push(NodeUtils.stackSizeToPos(node.getSize()));
      }
   }

   @Override
   public void outAActionCommand(AActionCommand node) {
      if (!this.skipdeadcode) {
         int remove = NodeUtils.actionRemoveElementCount(node, this.actions);
         Type rettype = NodeUtils.getReturnType(node, this.actions);
         int add;
         try {
            add = NodeUtils.stackSizeToPos(rettype.typeSize());
         } catch (RuntimeException e) {
            add = 1;
         }
         this.pop(remove);
         this.push(add);
      }
   }

   @Override
   public void outALogiiCommand(ALogiiCommand node) {
      if (!this.skipdeadcode) {
         this.pop(2);
         this.push(1);
      }
   }

   @Override
   public void outABinaryCommand(ABinaryCommand node) {
      if (!this.skipdeadcode) {
         int sizep1;
         int sizep2;
         int sizeresult;
         if (NodeUtils.isEqualityOp(node)) {
            if (NodeUtils.getType(node).equals((byte)36)) {
               sizep1 = sizep2 = NodeUtils.stackSizeToPos(node.getSize());
            } else {
               sizep1 = 1;
               sizep2 = 1;
            }

            sizeresult = 1;
         } else if (NodeUtils.isVectorAllowedOp(node)) {
            sizep1 = NodeUtils.getParam1Size(node);
            sizep2 = NodeUtils.getParam2Size(node);
            sizeresult = NodeUtils.getResultSize(node);
         } else {
            sizep1 = 1;
            sizep2 = 1;
            sizeresult = 1;
         }

         this.pop(sizep1 + sizep2);
         this.push(sizeresult);
      }
   }

   @Override
   public void outAConditionalJumpCommand(AConditionalJumpCommand node) {
      if (!this.skipdeadcode) {
         this.pop(1);
      }
   }

   @Override
   public void outAJumpCommand(AJumpCommand node) {
      if (!this.skipdeadcode && NodeUtils.getJumpDestinationPos(node) < this.nodedata.getPos(node)) {
         this.resetGrowth();
      }
   }

   @Override
   public void outAJumpToSubroutine(AJumpToSubroutine node) {
      if (!this.skipdeadcode) {
         int dest = NodeUtils.getJumpDestinationPos(node);
         int inferred = Math.max(0, this.growth);
         if (inferred == 0) {
            inferred = Math.max(0, this.height);
         }

         this.inferredParams.merge(dest, inferred, Math::max);
         this.pop(inferred);
         this.resetGrowth();
      }
   }

   @Override
   public void outAMoveSpCommand(AMoveSpCommand node) {
      if (!this.skipdeadcode) {
         this.pop(NodeUtils.stackOffsetToPos(node.getOffset()));
         this.resetGrowth();
      }
   }

   @Override
   public void outADestructCommand(ADestructCommand node) {
      if (!this.skipdeadcode) {
         this.pop(NodeUtils.stackSizeToPos(node.getSizeRem()));
         this.resetGrowth();
      }
   }

   private void push(int count) {
      if (count <= 0) {
         return;
      }

      this.height += count;
      this.growth += count;
   }

   private void pop(int count) {
      if (count <= 0) {
         return;
      }

      this.height = Math.max(0, this.height - count);
      this.growth = Math.max(0, this.growth - count);
   }

   private void resetGrowth() {
      this.growth = 0;
   }
}
