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
import com.kotor.resource.formats.ncs.node.AProgram;
import com.kotor.resource.formats.ncs.node.AReturn;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.AStackCommand;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.AUnaryCommand;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.scriptnode.ASub;
import com.kotor.resource.formats.ncs.scriptutils.SubScriptState;
import com.kotor.resource.formats.ncs.stack.Const;
import com.kotor.resource.formats.ncs.stack.LocalVarStack;
import com.kotor.resource.formats.ncs.stack.StackEntry;
import com.kotor.resource.formats.ncs.stack.VarStruct;
import com.kotor.resource.formats.ncs.stack.Variable;
import com.kotor.resource.formats.ncs.utils.NodeAnalysisData;
import com.kotor.resource.formats.ncs.utils.NodeUtils;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import com.kotor.resource.formats.ncs.utils.SubroutineState;
import com.kotor.resource.formats.ncs.utils.Type;
import java.util.ArrayList;

/**
 * Second-phase pass that converts the annotated parse tree into script text.
 * <p>
 * It walks the AST, mirrors stack effects, tracks globals vs local scopes, and
 * delegates to {@link SubScriptState} to build readable code (prototypes,
 * bodies, globals). Dead code can be skipped based on earlier analysis.
 */
public class MainPass extends PrunedDepthFirstAdapter {
   /** Live variable stack reflecting current execution point. */
   protected LocalVarStack stack = new LocalVarStack();
   protected NodeAnalysisData nodedata;
   protected SubroutineAnalysisData subdata;
   protected boolean skipdeadcode;
   /** Mutable script output for the current subroutine. */
   protected SubScriptState state;
   private ActionsData actions;
   /** Whether we are operating on the globals block. */
   protected boolean globals;
   /** Backup stack used around jumps to restore state. */
   protected LocalVarStack backupstack;
   /** Declared return type of the current subroutine. */
   protected Type type;

   public MainPass(SubroutineState state, NodeAnalysisData nodedata, SubroutineAnalysisData subdata, ActionsData actions) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.actions = actions;
      state.initStack(this.stack);
      this.skipdeadcode = false;
      this.state = new SubScriptState(nodedata, subdata, this.stack, state, actions, FileDecompiler.preferSwitches);
      this.globals = false;
      this.backupstack = null;
      this.type = state.type();
   }

   protected MainPass(NodeAnalysisData nodedata, SubroutineAnalysisData subdata) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.skipdeadcode = false;
      this.state = new SubScriptState(nodedata, subdata, this.stack, FileDecompiler.preferSwitches);
      this.globals = true;
      this.backupstack = null;
      this.type = new Type((byte)-1);
   }

   public void done() {
      this.stack = null;
      this.nodedata = null;
      this.subdata = null;
      if (this.state != null) {
         this.state.parseDone();
      }

      this.state = null;
      this.actions = null;
      this.backupstack = null;
      this.type = null;
   }

   public void assertStack() {
      if ((this.type.equals((byte)0) || this.type.equals((byte)-1)) && this.stack.size() > 0) {
         throw new RuntimeException("Error: Final stack size " + Integer.toString(this.stack.size()) + this.stack.toString());
      }
   }

   public String getCode() {
      return this.state.toString();
   }

   public String getProto() {
      return this.state.getProto();
   }

   public ASub getScriptRoot() {
      return this.state.getRoot();
   }

   public SubScriptState getState() {
      return this.state;
   }

   @Override
   public void outARsaddCommand(ARsaddCommand node) {
      if (!this.skipdeadcode) {
         Variable var = new Variable(NodeUtils.getType(node));
         this.stack.push(var);
         var = null;
         this.state.transformRSAdd(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outACopyDownSpCommand(ACopyDownSpCommand node) {
      if (!this.skipdeadcode) {
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());
         if (copy > 1) {
            this.stack.structify(loc - copy + 1, copy, this.subdata);
         }

         this.state.transformCopyDownSp(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outACopyTopSpCommand(ACopyTopSpCommand node) {
      if (!this.skipdeadcode) {
         VarStruct varstruct = null;
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());
         if (copy > 1) {
            varstruct = this.stack.structify(loc - copy + 1, copy, this.subdata);
         }

         this.state.transformCopyTopSp(node);
         if (copy > 1) {
            this.stack.push(varstruct);
         } else {
            for (int i = 0; i < copy; i++) {
               StackEntry entry = this.stack.get(loc);
               this.stack.push(entry);
            }
         }
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAConstCommand(AConstCommand node) {
      if (!this.skipdeadcode) {
         Type type = NodeUtils.getType(node);
         Const aconst;
         switch (type.byteValue()) {
            case 3:
               aconst = Const.newConst(type, NodeUtils.getIntConstValue(node));
               break;
            case 4:
               aconst = Const.newConst(type, NodeUtils.getFloatConstValue(node));
               break;
            case 5:
               aconst = Const.newConst(type, NodeUtils.getStringConstValue(node));
               break;
            case 6:
               aconst = Const.newConst(type, NodeUtils.getObjectConstValue(node));
               break;
            default:
               throw new RuntimeException("Invalid const type " + type);
         }
         this.stack.push(aconst);
         this.state.transformConst(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAActionCommand(AActionCommand node) {
      if (!this.skipdeadcode) {
         int remove = NodeUtils.actionRemoveElementCount(node, this.actions);
         int i = 0;

         while (i < remove) {
            StackEntry entry = this.removeFromStack();
            i += entry.size();
         }

         Type type = NodeUtils.getReturnType(node, this.actions);
         if (!type.equals((byte)-16)) {
            if (!type.equals((byte)0)) {
               Variable var = new Variable(type);
               this.stack.push(var);
            }
         } else {
            for (int ix = 0; ix < 3; ix++) {
               Variable var = new Variable((byte)4);
               this.stack.push(var);
            }

            this.stack.structify(1, 3, this.subdata);
         }

         this.state.transformAction(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outALogiiCommand(ALogiiCommand node) {
      if (!this.skipdeadcode) {
         this.removeFromStack();
         this.removeFromStack();
         Variable var = new Variable((byte)3);
         this.stack.push(var);
         var = null;
         this.state.transformLogii(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outABinaryCommand(ABinaryCommand node) {
      if (!this.skipdeadcode) {
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

         for (int i = 0; i < sizep1 + sizep2; i++) {
            this.removeFromStack();
         }

         for (int i = 0; i < sizeresult; i++) {
            Variable var = new Variable(resulttype);
            this.stack.push(var);
         }

         this.state.transformBinary(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAUnaryCommand(AUnaryCommand node) {
      if (!this.skipdeadcode) {
         this.state.transformUnary(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAMoveSpCommand(AMoveSpCommand node) {
      if (!this.skipdeadcode) {
         this.state.transformMoveSp(node);
         this.backupstack = (LocalVarStack)this.stack.clone();
         int remove = NodeUtils.stackOffsetToPos(node.getOffset());
         ArrayList<Variable> entries = new ArrayList<>();
         int i = 0;

         while (i < remove) {
            StackEntry entry = this.removeFromStack();
            i += entry.size();
            if (Variable.class.isInstance(entry) && !((Variable)entry).isPlaceholder(this.stack) && !((Variable)entry).isOnStack(this.stack)) {
               entries.add((Variable)entry);
            }
         }

         if (entries.size() > 0 && !this.nodedata.deadCode(node)) {
            this.state.transformMoveSPVariablesRemoved(entries, node);
         }
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAConditionalJumpCommand(AConditionalJumpCommand node) {
      if (!this.skipdeadcode) {
         if (this.nodedata.logOrCode(node)) {
            this.state.transformLogOrExtraJump(node);
         } else {
            this.state.transformConditionalJump(node);
         }

         this.removeFromStack();
         if (!this.nodedata.logOrCode(node)) {
            this.storeStackState(this.nodedata.getDestination(node), this.nodedata.deadCode(node));
         }
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAJumpCommand(AJumpCommand node) {
      if (!this.skipdeadcode) {
         this.state.transformJump(node);
         this.storeStackState(this.nodedata.getDestination(node), this.nodedata.deadCode(node));
         if (this.backupstack != null) {
            this.stack.doneWithStack();
            this.stack = this.backupstack;
            this.state.setStack(this.stack);
         }
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAJumpToSubroutine(AJumpToSubroutine node) {
      if (!this.skipdeadcode) {
         SubroutineState substate = this.subdata.getState(this.nodedata.getDestination(node));
         int paramsize = substate.getParamCount();

         for (int i = 0; i < paramsize; i++) {
            this.removeFromStack();
         }

         this.state.transformJSR(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outADestructCommand(ADestructCommand node) {
      if (!this.skipdeadcode) {
         this.state.transformDestruct(node);
         int removesize = NodeUtils.stackSizeToPos(node.getSizeRem());
         int savestart = NodeUtils.stackSizeToPos(node.getOffset());
         int savesize = NodeUtils.stackSizeToPos(node.getSizeSave());
         this.stack.destruct(removesize, savestart, savesize, this.subdata);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outACopyTopBpCommand(ACopyTopBpCommand node) {
      if (!this.skipdeadcode) {
         VarStruct varstruct = null;
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());
         if (copy > 1) {
            varstruct = this.subdata.getGlobalStack().structify(loc - copy + 1, copy, this.subdata);
         }

         this.state.transformCopyTopBp(node);
         if (copy > 1) {
            this.stack.push(varstruct);
         } else {
            for (int i = 0; i < copy; i++) {
               Variable var = (Variable)this.subdata.getGlobalStack().get(loc);
               this.stack.push(var);
               loc--;
            }
         }

      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outACopyDownBpCommand(ACopyDownBpCommand node) {
      if (!this.skipdeadcode) {
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());
         if (copy > 1) {
            this.subdata.getGlobalStack().structify(loc - copy + 1, copy, this.subdata);
         }

         this.state.transformCopyDownBp(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAStoreStateCommand(AStoreStateCommand node) {
      if (!this.skipdeadcode) {
         this.state.transformStoreState(node);
         this.backupstack = null;
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAStackCommand(AStackCommand node) {
      if (!this.skipdeadcode) {
         this.state.transformStack(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outAReturn(AReturn node) {
      if (!this.skipdeadcode) {
         this.state.transformReturn(node);
      } else {
         this.state.transformDeadCode(node);
      }
   }

   @Override
   public void outASubroutine(ASubroutine node) {
   }

   @Override
   public void outAProgram(AProgram node) {
   }

   @Override
   public void defaultIn(Node node) {
      this.restoreStackState(node);
      this.checkOrigins(node);
      if (NodeUtils.isCommandNode(node)) {
         this.skipdeadcode = !this.nodedata.processCode(node);
      }
   }

   private StackEntry removeFromStack() {
      StackEntry entry = this.stack.remove();
      if (Variable.class.isInstance(entry) && ((Variable)entry).isPlaceholder(this.stack)) {
         this.state.transformPlaceholderVariableRemoved((Variable)entry);
      }

      return entry;
   }

   private void storeStackState(Node node, boolean isdead) {
      if (NodeUtils.isStoreStackNode(node)) {
         this.nodedata.setStack(node, (LocalVarStack)this.stack.clone(), false);
      }
   }

   private void restoreStackState(Node node) {
      LocalVarStack restore = (LocalVarStack)this.nodedata.getStack(node);
      if (restore != null) {
         this.stack.doneWithStack();
         this.stack = restore;
         this.state.setStack(this.stack);
         if (this.backupstack != null) {
            this.backupstack.doneWithStack();
         }

         this.backupstack = null;
      }

      restore = null;
   }

   private void checkOrigins(Node node) {
      Node origin;
      while ((origin = this.getNextOrigin(node)) != null) {
         this.state.transformOriginFound(node, origin);
      }

      origin = null;
   }

   private Node getNextOrigin(Node node) {
      return this.nodedata.removeLastOrigin(node);
   }
}

