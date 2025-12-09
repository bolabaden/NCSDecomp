// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import com.kotor.resource.formats.ncs.node.ABpCommand;
import com.kotor.resource.formats.ncs.node.ACopyDownSpCommand;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.AMoveSpCommand;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.stack.LocalVarStack;
import com.kotor.resource.formats.ncs.stack.Variable;
import com.kotor.resource.formats.ncs.utils.NodeAnalysisData;
import com.kotor.resource.formats.ncs.utils.NodeUtils;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;

/**
 * Specialized {@link MainPass} for globals subroutine handling.
 * <p>
 * Globals use BP-based stack ops that can invalidate offsets; this pass freezes
 * stack mutations once BP ops are seen, while still emitting globals code.
 */
public class DoGlobalVars extends MainPass {
   private boolean freezeStack;

   public DoGlobalVars(NodeAnalysisData nodedata, SubroutineAnalysisData subdata) {
      super(nodedata, subdata);
      this.state.setVarPrefix("GLOB_");
      this.freezeStack = false;
   }

   @Override
   public String getCode() {
      return this.state.toStringGlobals();
   }

   @Override
   public void outABpCommand(ABpCommand node) {
      this.freezeStack = true;
   }

   @Override
   public void outAJumpToSubroutine(AJumpToSubroutine node) {
      this.freezeStack = true;
   }

   @Override
   public void outAMoveSpCommand(AMoveSpCommand node) {
      if (!this.freezeStack) {
         this.state.transformMoveSp(node);
         int remove = NodeUtils.stackOffsetToPos(node.getOffset());

         for (int i = 0; i < remove; i++) {
            this.stack.remove();
         }
      }
   }

   @Override
   public void outACopyDownSpCommand(ACopyDownSpCommand node) {
      if (!this.freezeStack) {
         this.state.transformCopyDownSp(node);
      }
   }

   @Override
   public void outARsaddCommand(ARsaddCommand node) {
      if (!this.freezeStack) {
         Variable var = new Variable(NodeUtils.getType(node));
         this.stack.push(var);
         this.state.transformRSAdd(node);
         var = null;
      }
   }

   public LocalVarStack getStack() {
      return this.stack;
   }
}

