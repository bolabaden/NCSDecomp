// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import com.kotor.resource.formats.ncs.analysis.PrunedDepthFirstAdapter;
import com.kotor.resource.formats.ncs.node.AActionCommand;
import com.kotor.resource.formats.ncs.node.ABinaryCommand;
import com.kotor.resource.formats.ncs.node.AConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.AConstCommand;
import com.kotor.resource.formats.ncs.node.ACopyDownBpCommand;
import com.kotor.resource.formats.ncs.node.ACopyDownSpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopBpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopSpCommand;
import com.kotor.resource.formats.ncs.node.ADestructCommand;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.ALogiiCommand;
import com.kotor.resource.formats.ncs.node.AMoveSpCommand;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.stack.LocalTypeStack;
import com.kotor.resource.formats.ncs.utils.NodeAnalysisData;
import com.kotor.resource.formats.ncs.utils.NodeUtils;
import com.kotor.resource.formats.ncs.utils.StructType;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import com.kotor.resource.formats.ncs.utils.SubroutineState;
import com.kotor.resource.formats.ncs.utils.Type;

/**
 * First-phase pass that infers stack types and prototypes subroutines.
 * <p>
 * Traverses the AST to determine parameter counts, return types, and local
 * structures before code emission. Supports an "initial prototype" mode that
 * relaxes typing requirements to bootstrap later passes.
 */
public class DoTypes extends PrunedDepthFirstAdapter {
   private SubroutineState state;
   /** Type-only view of the execution stack for inference. */
   protected LocalTypeStack stack = new LocalTypeStack();
   private NodeAnalysisData nodedata;
   private SubroutineAnalysisData subdata;
   private ActionsData actions;
   /** Whether we are in the first prototyping pass. */
   private boolean initialproto;
   /** True while temporarily skipping sections during prototyping. */
   private boolean protoskipping;
   /** Whether we should emit return type information during this pass. */
   private boolean protoreturn;
   /** Skip nodes flagged as dead code. */
   private boolean skipdeadcode;
   /** Backup stack used around jumps for restoration. */
   private LocalTypeStack backupstack;

   public DoTypes(SubroutineState state, NodeAnalysisData nodedata, SubroutineAnalysisData subdata, ActionsData actions, boolean initialprototyping) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.state = state;
      this.actions = actions;
      if (!initialprototyping) {
         this.state.initStack(this.stack);
      }

      this.initialproto = initialprototyping;
      this.protoskipping = false;
      this.skipdeadcode = false;
      this.protoreturn = this.initialproto || !state.type().isTyped();
   }

   public void done() {
      this.state = null;
      if (this.stack != null) {
         this.stack.close();
         this.stack = null;
      }

      this.nodedata = null;
      this.subdata = null;
      if (this.backupstack != null) {
         this.backupstack.close();
         this.backupstack = null;
      }

      this.actions = null;
   }

   public void assertStack() {
      if (this.stack.size() > 0) {
         System.out.println("Uh-oh... dumping main() state:");
         this.state.printState();
         throw new RuntimeException("Error: Final stack size " + Integer.toString(this.stack.size()));
      }
   }

   @Override
   public void outARsaddCommand(ARsaddCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         this.stack.push(NodeUtils.getType(node));
      }
   }

   @Override
   public void outACopyDownSpCommand(ACopyDownSpCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());
         boolean isstruct = copy > 1;
         if (this.protoreturn && loc > this.stack.size()) {
            if (isstruct) {
               StructType struct = new StructType();

               for (int i = copy; i >= 1; i--) {
                  struct.addType(this.stack.get(i));
               }

               this.state.setReturnType(struct, loc - this.stack.size());
               this.subdata.addStruct(struct);
            } else {
               this.state.setReturnType(this.stack.get(1, this.state), loc - this.stack.size());
            }
         }
      }
   }

   @Override
   public void outACopyTopSpCommand(ACopyTopSpCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());

         for (int i = 0; i < copy; i++) {
            this.stack.push(this.stack.get(loc, this.state));
         }
      }
   }

   @Override
   public void outAConstCommand(AConstCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         this.stack.push(NodeUtils.getType(node));
      }
   }

   @Override
   public void outAActionCommand(AActionCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         int remove = NodeUtils.actionRemoveElementCount(node, this.actions);
         Type type = NodeUtils.getReturnType(node, this.actions);
         int add = NodeUtils.stackSizeToPos(type.typeSize());
         this.stack.remove(remove);

         for (int i = 0; i < add; i++) {
            this.stack.push(type);
         }
      }
   }

   @Override
   public void outALogiiCommand(ALogiiCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         this.stack.remove(2);
         this.stack.push(new Type((byte)3));
      }
   }

   @Override
   public void outABinaryCommand(ABinaryCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         int sizep1;
         int sizep2;
         int sizeresult;
         Type resulttype;
         if (NodeUtils.isEqualityOp(node)) {
            if (NodeUtils.getType(node).equals((byte)36)) {
               sizep1 = sizep2 = NodeUtils.stackSizeToPos(node.getSize());
            } else {
               sizep2 = 1;
               sizep1 = 1;
            }

            sizeresult = 1;
            resulttype = new Type((byte)3);
         } else if (NodeUtils.isVectorAllowedOp(node)) {
            sizep1 = NodeUtils.getParam1Size(node);
            sizep2 = NodeUtils.getParam2Size(node);
            sizeresult = NodeUtils.getResultSize(node);
            resulttype = NodeUtils.getReturnType(node);
         } else {
            sizep1 = 1;
            sizep2 = 1;
            sizeresult = 1;
            resulttype = new Type((byte)3);
         }

         this.stack.remove(sizep1 + sizep2);

         for (int i = 0; i < sizeresult; i++) {
            this.stack.push(resulttype);
         }
      }
   }

   @Override
   public void outAMoveSpCommand(AMoveSpCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         if (this.initialproto) {
            int params = this.stack.removePrototyping(NodeUtils.stackOffsetToPos(node.getOffset()));
            if (params > 0) {
               this.state.setParamCount(params);
            }
         } else {
            this.stack.remove(NodeUtils.stackOffsetToPos(node.getOffset()));
         }
      }
   }

   @Override
   public void outAStoreStateCommand(AStoreStateCommand node) {
   }

   @Override
   public void outAConditionalJumpCommand(AConditionalJumpCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         this.stack.remove(1);
      }

      this.checkProtoskippingStart(node);
      if (!this.protoskipping && !this.skipdeadcode && !this.isLogOr(node)) {
         this.storeStackState(this.nodedata.getDestination(node));
      }
   }

   @Override
   public void outAJumpCommand(AJumpCommand node) {
      this.checkProtoskippingStart(node);
      if (!this.protoskipping && !this.skipdeadcode) {
         this.storeStackState(this.nodedata.getDestination(node));
      }
   }

   @Override
   public void outAJumpToSubroutine(AJumpToSubroutine node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         SubroutineState substate = this.subdata.getState(this.nodedata.getDestination(node));
         if (!substate.isPrototyped()) {
            System.out.println("Uh-oh...");
            substate.printState();
            throw new RuntimeException("Hit JSR on unprototyped subroutine " + Integer.toString(this.nodedata.getPos(this.nodedata.getDestination(node))));
         }

         int paramsize = substate.getParamCount();
         if (substate.isTotallyPrototyped()) {
            this.stack.remove(paramsize);
         } else {
            this.stack.removeParams(paramsize, substate);
            if (substate.type().equals((byte)-1)) {
               substate.setReturnType(this.stack.get(1, this.state), 0);
            }

            if (substate.type().equals((byte)-15) && !substate.type().isTyped()) {
               for (int i = 0; i < substate.type().size(); i++) {
                  Type type = this.stack.get(substate.type().size() - i, this.state);
                  if (!type.equals((byte)-1)) {
                     ((StructType)substate.type()).updateType(i, type);
                  }
               }
            }
         }
      }
   }

   @Override
   public void outADestructCommand(ADestructCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         int removesize = NodeUtils.stackSizeToPos(node.getSizeRem());
         int savestart = NodeUtils.stackSizeToPos(node.getOffset());
         int savesize = NodeUtils.stackSizeToPos(node.getSizeSave());
         this.stack.remove(removesize - (savesize + savestart));
         this.stack.remove(savesize + 1, savestart);
      }
   }

   @Override
   public void outACopyTopBpCommand(ACopyTopBpCommand node) {
      if (!this.protoskipping && !this.skipdeadcode) {
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());

         for (int i = 0; i < copy; i++) {
            this.stack.push(this.subdata.getGlobalStack().getType(loc));
            loc--;
         }
      }
   }

   @Override
   public void outACopyDownBpCommand(ACopyDownBpCommand node) {
   }

   @Override
   public void outASubroutine(ASubroutine node) {
      if (this.initialproto) {
         this.state.stopPrototyping(true);
      }
   }

   @Override
   public void defaultIn(Node node) {
      if (!this.protoskipping) {
         this.restoreStackState(node);
      } else {
         this.checkProtoskippingDone(node);
      }

      if (NodeUtils.isCommandNode(node)) {
         this.skipdeadcode = this.nodedata.deadCode(node);
      }
   }

   private void checkProtoskippingDone(Node node) {
      if (this.state.getSkipEnd(this.nodedata.getPos(node))) {
         this.protoskipping = false;
      }
   }

   private void checkProtoskippingStart(Node node) {
      if (this.state.getSkipStart(this.nodedata.getPos(node))) {
         this.protoskipping = true;
      }
   }

   private void storeStackState(Node node) {
      if (NodeUtils.isStoreStackNode(node)) {
         this.nodedata.setStack(node, (LocalTypeStack)this.stack.clone(), true);
      }
   }

   private void restoreStackState(Node node) {
      LocalTypeStack restore = (LocalTypeStack)this.nodedata.getStack(node);
      if (restore != null) {
         this.stack = restore;
      }
   }

   private boolean isLogOr(Node node) {
      return this.nodedata.logOrCode(node);
   }
}

