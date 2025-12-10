// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptutils;

import com.kotor.resource.formats.ncs.ActionsData;
import com.kotor.resource.formats.ncs.node.AActionCommand;
import com.kotor.resource.formats.ncs.node.ABinaryCommand;
import com.kotor.resource.formats.ncs.node.ABpCommand;
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
import com.kotor.resource.formats.ncs.node.AReturn;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.AStackCommand;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.AUnaryCommand;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.scriptnode.AActionArgExp;
import com.kotor.resource.formats.ncs.scriptnode.AActionExp;
import com.kotor.resource.formats.ncs.scriptnode.ABinaryExp;
import com.kotor.resource.formats.ncs.scriptnode.ABreakStatement;
import com.kotor.resource.formats.ncs.scriptnode.ACodeBlock;
import com.kotor.resource.formats.ncs.scriptnode.AConditionalExp;
import com.kotor.resource.formats.ncs.scriptnode.AConst;
import com.kotor.resource.formats.ncs.scriptnode.AContinueStatement;
import com.kotor.resource.formats.ncs.scriptnode.AControlLoop;
import com.kotor.resource.formats.ncs.scriptnode.ADoLoop;
import com.kotor.resource.formats.ncs.scriptnode.AElse;
import com.kotor.resource.formats.ncs.scriptnode.AExpression;
import com.kotor.resource.formats.ncs.scriptnode.AExpressionStatement;
import com.kotor.resource.formats.ncs.scriptnode.AFcnCallExp;
import com.kotor.resource.formats.ncs.scriptnode.AIf;
import com.kotor.resource.formats.ncs.scriptnode.AModifyExp;
import com.kotor.resource.formats.ncs.scriptnode.AReturnStatement;
import com.kotor.resource.formats.ncs.scriptnode.ASub;
import com.kotor.resource.formats.ncs.scriptnode.ASwitch;
import com.kotor.resource.formats.ncs.scriptnode.ASwitchCase;
import com.kotor.resource.formats.ncs.scriptnode.AUnaryExp;
import com.kotor.resource.formats.ncs.scriptnode.AUnaryModExp;
import com.kotor.resource.formats.ncs.scriptnode.AUnkLoopControl;
import com.kotor.resource.formats.ncs.scriptnode.AVarDecl;
import com.kotor.resource.formats.ncs.scriptnode.AVarRef;
import com.kotor.resource.formats.ncs.scriptnode.AVectorConstExp;
import com.kotor.resource.formats.ncs.scriptnode.AWhileLoop;
import com.kotor.resource.formats.ncs.scriptnode.ScriptNode;
import com.kotor.resource.formats.ncs.scriptnode.ScriptRootNode;
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Mutable builder that converts analysis results into script-level AST nodes.
 * <p>
 * Tracks the current subroutine, manages variable naming/struct grouping, and
 * assembles expressions/statements as parser passes walk the bytecode-derived
 * tree.
 */
@SuppressWarnings({"unused"})
public class SubScriptState {
   private static final byte STATE_DONE = -1;
   private static final byte STATE_NORMAL = 0;
   private static final byte STATE_INMOD = 1;
   private static final byte STATE_INACTIONARG = 2;
   private static final byte STATE_WHILECOND = 3;
   private static final byte STATE_SWITCHCASES = 4;
   private static final byte STATE_INPREFIXSTACK = 5;
   private ASub root;
   private ScriptRootNode current;
   private byte state;
   private NodeAnalysisData nodedata;
   private SubroutineAnalysisData subdata;
   private ActionsData actions;
   private LocalVarStack stack;
   private String varprefix;
   private Hashtable<Variable, AVarDecl> vardecs;
   private Hashtable<Type, Integer> varcounts;
   private Hashtable<String, Integer> varnames;

   public SubScriptState(NodeAnalysisData nodedata, SubroutineAnalysisData subdata, LocalVarStack stack,
         SubroutineState protostate, ActionsData actions) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.state = 0;
      this.vardecs = new Hashtable<>(1);
      this.stack = stack;
      this.varcounts = new Hashtable<>(1);
      this.varprefix = "";
      this.root = new ASub(protostate.type(), protostate.getId(), this.getParams(protostate.getParamCount()),
            protostate.getStart(), protostate.getEnd());
      this.current = this.root;
      this.varnames = new Hashtable<>(1);
      this.actions = actions;
   }

   public SubScriptState(NodeAnalysisData nodedata, SubroutineAnalysisData subdata, LocalVarStack stack) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.state = 0;
      this.vardecs = new Hashtable<>(1);
      this.root = new ASub(0, 0);
      this.current = this.root;
      this.stack = stack;
      this.varcounts = new Hashtable<>(1);
      this.varprefix = "";
      this.varnames = new Hashtable<>(1);
   }

   public void setVarPrefix(String prefix) {
      this.varprefix = prefix;
   }

   public void setStack(LocalVarStack stack) {
      this.stack = stack;
   }

   public void parseDone() {
      this.nodedata = null;
      this.subdata = null;
      if (this.stack != null) {
         this.stack.doneParse();
      }

      this.stack = null;
      if (this.vardecs != null) {
         Enumeration<Variable> en = this.vardecs.keys();

         while (en.hasMoreElements()) {
            Variable var = en.nextElement();
            var.doneParse();
         }
      }

      Enumeration<Variable> en = null;
   }

   public void close() {
      if (this.vardecs != null) {
         Enumeration<Variable> en = this.vardecs.keys();

         while (en.hasMoreElements()) {
            Variable var = en.nextElement();
            var.close();
         }

         this.vardecs = null;
      }

      this.varcounts = null;
      this.varnames = null;
      if (this.root != null) {
         this.root.close();
      }

      this.current = null;
      this.root = null;
      this.nodedata = null;
      this.subdata = null;
      this.actions = null;
      if (this.stack != null) {
         this.stack.close();
         this.stack = null;
      }

      Enumeration<Variable> en = null;
   }

   @Override
   public String toString() {
      return this.root.toString();
   }

   public String toStringGlobals() {
      return this.root.getBody();
   }

   public String getProto() {
      return this.root.getHeader();
   }

   public ASub getRoot() {
      return this.root;
   }

   public String getName() {
      return this.root.name();
   }

   public void setName(String name) {
      this.root.name(name);
   }

   public Vector<Variable> getVariables() {
      Vector<Variable> vars = new Vector<>(this.vardecs.keySet());
      TreeSet<VarStruct> varstructs = new TreeSet<>();
      Iterator<Variable> it = vars.iterator();

      while (it.hasNext()) {
         Variable var = it.next();
         if (var.isStruct()) {
            varstructs.add(var.varstruct());
            it.remove();
         }
      }

      vars.addAll(varstructs);
      vars.addAll(this.root.getParamVars());
      return vars;
   }

   public void isMain(boolean ismain) {
      this.root.isMain(ismain);
   }

   public boolean isMain() {
      return this.root.isMain();
   }

   private void assertState(Node node) {
      if (this.state != 0) {
         if (this.state == 2 && !AJumpCommand.class.isInstance(node)) {
            throw new RuntimeException("In action arg, expected JUMP at node " + node);
         } else if (this.state == -1) {
            throw new RuntimeException("In DONE state, no more nodes expected at node " + node);
         } else if (this.state == 5 && !ACopyTopSpCommand.class.isInstance(node)) {
            throw new RuntimeException("In prefix stack op state, expected CPTOPSP at node " + node);
         }
      }
   }

   private void checkStart(Node node) {
      this.assertState(node);
      if (this.current.hasChildren()) {
         ScriptNode lastNode = this.current.getLastChild();
         if (ASwitch.class.isInstance(lastNode)
               && this.nodedata.getPos(node) == ((ASwitch) lastNode).getFirstCaseStart()) {
            this.current = ((ASwitch) lastNode).getFirstCase();
         }
      }

      ScriptNode lastNode = null;
   }

   private void checkEnd(Node node) {
      while (this.current != null) {
         if (this.nodedata.getPos(node) != this.current.getEnd()) {
            return;
         }

         if (ASwitchCase.class.isInstance(this.current)) {
            ASwitchCase nextCase = ((ASwitch) this.current.parent()).getNextCase((ASwitchCase) this.current);
            if (nextCase != null) {
               this.current = nextCase;
            } else {
               this.current = (ScriptRootNode) this.current.parent().parent();
            }

            nextCase = null;
            return;
         }

         if (AIf.class.isInstance(this.current)) {
            Node dest = this.nodedata.getDestination(node);
            if (dest == null) {
               return;
            }

            if (this.nodedata.getPos(dest) != this.current.getEnd() + 6) {
               AElse aelse = new AElse(this.current.getEnd() + 6,
                     this.nodedata.getPos(NodeUtils.getPreviousCommand(dest, this.nodedata)));
               this.current = (ScriptRootNode) this.current.parent();
               this.current.addChild(aelse);
               this.current = aelse;
               aelse = null;
               dest = null;
               return;
            }
         }

         if (ADoLoop.class.isInstance(this.current)) {
            this.transformEndDoLoop();
         }

         this.current = (ScriptRootNode) this.current.parent();
      }

      this.state = -1;
   }

   public boolean inActionArg() {
      return this.state == 2;
   }

   public void transformPlaceholderVariableRemoved(Variable var) {
      AVarDecl vardec = this.vardecs.get(var);
      if (vardec != null && vardec.isFcnReturn()) {
         Object exp = vardec.exp();
         ScriptRootNode parent = (ScriptRootNode) vardec.parent();
         if (exp != null) {
            parent.replaceChild(vardec, (ScriptNode) exp);
         } else {
            parent.removeChild(vardec);
         }

         ScriptRootNode var6 = null;
         this.vardecs.remove(var);
      }

      AVarDecl var8 = null;
      ScriptRootNode parent = null;
      Object exp = null;
   }

   private boolean removingSwitchVar(List<Variable> vars, Node node) {
      if (vars.size() == 1 && this.current.hasChildren() && ASwitch.class.isInstance(this.current.getLastChild())) {
         AExpression exp = ((ASwitch) this.current.getLastChild()).switchExp();
         return AVarRef.class.isInstance(exp) && ((AVarRef) exp).var().equals(vars.get(0));
      } else {
         return false;
      }
   }

   public void transformMoveSPVariablesRemoved(List<Variable> vars, Node node) {
      if (!this.atLastCommand(node) || !this.currentContainsVars(vars)) {
         if (vars.size() != 0) {
            if (!this.isMiddleOfReturn(node)) {
               if (!this.removingSwitchVar(vars, node)) {
                  if (this.currentContainsVars(vars)) {
                     int earliestdec = -1;

                     for (Variable var : vars) {
                        AVarDecl vardec = this.vardecs.get(var);
                        earliestdec = this.getEarlierDec(vardec, earliestdec);
                     }

                     if (earliestdec != -1) {
                        Node prev = NodeUtils.getPreviousCommand(node, this.nodedata);
                        ACodeBlock block = new ACodeBlock(-1, this.nodedata.getPos(prev));
                        List<ScriptNode> children = this.current.removeChildren(earliestdec);
                        this.current.addChild(block);
                        block.addChildren(children);
                        children = null;
                        ACodeBlock var13 = null;
                        prev = null;
                     }

                     Variable var = null;
                     AVarDecl vardec = null;
                  }
               }
            }
         }
      }
   }

   public void transformEndDoLoop() {
      ((ADoLoop) this.current).condition(this.removeLastExp(false));
   }

   public void transformOriginFound(Node destination, Node origin) {
      AControlLoop loop = this.getLoop(destination, origin);
      this.current.addChild(loop);
      this.current = loop;
      if (AWhileLoop.class.isInstance(loop)) {
         this.state = 3;
      }

      loop = null;
   }

   public void transformLogOrExtraJump(AConditionalJumpCommand node) {
      this.removeLastExp(true);
   }

   public void transformConditionalJump(AConditionalJumpCommand node) {
      this.checkStart(node);
      if (this.state == 3) {
         ((AWhileLoop) this.current).condition(this.removeLastExp(false));
         this.state = 0;
      } else if (!NodeUtils.isJz(node)) {
         if (this.state != 4) {
            AConditionalExp cond = (AConditionalExp) this.removeLastExp(true);
            ASwitch aswitch = null;
            ASwitchCase acase = new ASwitchCase(this.nodedata.getPos(this.nodedata.getDestination(node)),
                  (AConst) cond.right());
            if (this.current.hasChildren()) {
               ScriptNode last = this.current.getLastChild();
               if (AVarRef.class.isInstance(last) && AVarRef.class.isInstance(cond.left())
                     && ((AVarRef) last).var().equals(((AVarRef) cond.left()).var())) {
                  AVarRef varref = (AVarRef) this.removeLastExp(false);
                  aswitch = new ASwitch(this.nodedata.getPos(node), varref);
               }
            }

            if (aswitch == null) {
               aswitch = new ASwitch(this.nodedata.getPos(node), cond.left());
            }

            this.current.addChild(aswitch);
            aswitch.addCase(acase);
            this.state = 4;
         } else {
            AConditionalExp condx = (AConditionalExp) this.removeLastExp(true);
            ASwitch aswitchx = (ASwitch) this.current.getLastChild();
            ASwitchCase aprevcase = aswitchx.getLastCase();
            aprevcase.end(this.nodedata
                  .getPos(NodeUtils.getPreviousCommand(this.nodedata.getDestination(node), this.nodedata)));
            ASwitchCase acasex = new ASwitchCase(this.nodedata.getPos(this.nodedata.getDestination(node)),
                  (AConst) condx.right());
            aswitchx.addCase(acasex);
         }
      } else if (AIf.class.isInstance(this.current) && this.isModifyConditional()) {
         ((AIf) this.current).end(this.nodedata.getPos(this.nodedata.getDestination(node)) - 6);
         if (this.current.hasChildren()) {
            this.current.removeLastChild();
         }
      } else if (AWhileLoop.class.isInstance(this.current) && this.isModifyConditional()) {
         ((AWhileLoop) this.current).end(this.nodedata.getPos(this.nodedata.getDestination(node)) - 6);
         if (this.current.hasChildren()) {
            this.current.removeLastChild();
         }
      } else {
         AIf aif = new AIf(this.nodedata.getPos(node), this.nodedata.getPos(this.nodedata.getDestination(node)) - 6,
               this.removeLastExp(false));
         this.current.addChild(aif);
         this.current = aif;
      }

      this.checkEnd(node);
   }

   private boolean isModifyConditional() {
      if (!this.current.hasChildren()) {
         return true;
      } else if (this.current.size() == 1) {
         ScriptNode last = this.current.getLastChild();
         return AVarRef.class.isInstance(last) && !((AVarRef) last).var().isAssigned()
               && !((AVarRef) last).var().isParam();
      } else {
         return false;
      }
   }

   public void transformJump(AJumpCommand node) {
      this.checkStart(node);
      Node dest = this.nodedata.getDestination(node);
      if (this.state == 2) {
         this.state = 0;
         AActionArgExp aarg = new AActionArgExp(this.getNextCommand(node), this.getPriorToDestCommand(node));
         this.current.addChild(aarg);
         this.current = aarg;
      } else if (!AIf.class.isInstance(this.current) || this.nodedata.getPos(node) != this.current.getEnd()) {
         if (this.state == 4) {
            ASwitch aswitch = (ASwitch) this.current.getLastChild();
            ASwitchCase aprevcase = aswitch.getLastCase();
            if (aprevcase != null) {
               aprevcase.end(this.nodedata.getPos(NodeUtils.getPreviousCommand(dest, this.nodedata)));
            }

            if (AMoveSpCommand.class.isInstance(dest)) {
               aswitch.end(this.nodedata.getPos(this.nodedata.getDestination(node)));
            } else {
               ASwitchCase adefault = new ASwitchCase(this.nodedata.getPos(dest));
               aswitch.addDefaultCase(adefault);
            }

            this.state = 0;
         } else if (this.isReturn(node)) {
            AReturnStatement areturn;
            if (!this.root.type().equals((byte) 0)) {
               areturn = new AReturnStatement(this.getReturnExp());
            } else {
               areturn = new AReturnStatement();
            }

            this.current.addChild(areturn);
         } else if (this.nodedata.getPos(dest) >= this.nodedata.getPos(node)) {
            ScriptRootNode loop = this.getBreakable();
            if (ASwitchCase.class.isInstance(loop)) {
               loop = this.getEnclosingLoop(loop);
               if (loop == null) {
                  ABreakStatement abreak = new ABreakStatement();
                  this.current.addChild(abreak);
               } else {
                  AUnkLoopControl aunk = new AUnkLoopControl(this.nodedata.getPos(dest));
                  this.current.addChild(aunk);
               }
            } else if (loop != null && this.nodedata.getPos(dest) > loop.getEnd()) {
               ABreakStatement abreak = new ABreakStatement();
               this.current.addChild(abreak);
            } else {
               loop = this.getLoop();
               if (loop != null && this.nodedata.getPos(dest) <= loop.getEnd()) {
                  AContinueStatement acont = new AContinueStatement();
                  this.current.addChild(acont);
               }
            }
         }
      }

      this.checkEnd(node);
   }

   public void transformJSR(AJumpToSubroutine node) {
      this.checkStart(node);
      AFcnCallExp jsr = new AFcnCallExp(this.getFcnId(node), this.removeFcnParams(node));
      if (!this.getFcnType(node).equals((byte) 0)) {
         ((AVarDecl) this.current.getLastChild()).isFcnReturn(true);
         ((AVarDecl) this.current.getLastChild()).initializeExp(jsr);
         jsr.stackentry(this.stack.get(1));
      } else {
         this.current.addChild(jsr);
      }

      AFcnCallExp var3 = null;
      this.checkEnd(node);
   }

   public void transformAction(AActionCommand node) {
      this.checkStart(node);
      List<AExpression> params = this.removeActionParams(node);
      AActionExp act = new AActionExp(NodeUtils.getActionName(node, this.actions), NodeUtils.getActionId(node), params);
      Type type = NodeUtils.getReturnType(node, this.actions);
      if (!type.equals((byte) 0)) {
         Variable var = (Variable) this.stack.get(1);
         if (type.equals((byte) -16)) {
            var = var.varstruct();
         }

         act.stackentry(var);
         AVarDecl vardec = new AVarDecl(var);
         vardec.isFcnReturn(true);
         vardec.initializeExp(act);
         this.updateVarCount(var);
         this.current.addChild(vardec);
         this.vardecs.put(var, vardec);
      } else {
         this.current.addChild(act);
      }

      this.checkEnd(node);
   }

   public void transformReturn(AReturn node) {
      this.checkStart(node);
      this.checkEnd(node);
   }

   public void transformCopyDownSp(ACopyDownSpCommand node) {
      this.checkStart(node);
      AExpression exp = this.removeLastExp(false);
      if (this.isReturn(node)) {
         AReturnStatement ret = new AReturnStatement(exp);
         this.current.addChild(ret);
      } else {
         AVarRef varref = this.getVarToAssignTo(node);
         AModifyExp modexp = new AModifyExp(varref, exp);
         this.updateName(varref, exp);
         this.current.addChild(modexp);
         this.state = 1;
      }

      this.checkEnd(node);
   }

   private void updateName(AVarRef varref, AExpression exp) {
      if (AActionExp.class.isInstance(exp)) {
         String name = NameGenerator.getNameFromAction((AActionExp) exp);
         if (name != null && !this.varnames.containsKey(name)) {
            varref.var().name(name);
            this.varnames.put(name, Integer.valueOf(1));
         }
      }
   }

   public void transformCopyTopSp(ACopyTopSpCommand node) {
      this.checkStart(node);
      if (this.state == 5) {
         this.state = 0;
      } else {
         AExpression varref = this.getVarToCopy(node);
         this.current.addChild((ScriptNode) varref);
      }

      this.checkEnd(node);
   }

   public void transformCopyDownBp(ACopyDownBpCommand node) {
      this.checkStart(node);
      AVarRef varref = this.getVarToAssignTo(node);
      AExpression exp = this.removeLastExp(false);
      AModifyExp modexp = new AModifyExp(varref, exp);
      this.current.addChild(modexp);
      this.state = 1;
      this.checkEnd(node);
   }

   public void transformCopyTopBp(ACopyTopBpCommand node) {
      this.checkStart(node);
      AExpression varref = this.getVarToCopy(node);
      this.current.addChild((ScriptNode) varref);
      this.checkEnd(node);
   }

   public void transformMoveSp(AMoveSpCommand node) {
      this.checkStart(node);
      if (this.state == 1) {
         ScriptNode last = this.current.getLastChild();
         if (!AReturnStatement.class.isInstance(last)) {
            if (!AModifyExp.class.isInstance(last)) {
               System.out
                     .println("uh-oh... not a modify exp at " + this.nodedata.getPos(node) + ", " + last);
            }

            AModifyExp modexp = (AModifyExp) this.removeLastExp(true);
            AExpressionStatement stmt = new AExpressionStatement(modexp);
            this.current.addChild(stmt);
            stmt.parent(this.current);
         }

         this.state = 0;
      } else {
         this.checkSwitchEnd(node);
      }

      this.checkEnd(node);
   }

   public void transformRSAdd(ARsaddCommand node) {
      this.checkStart(node);
      Variable var = (Variable) this.stack.get(1);
      AVarDecl vardec = new AVarDecl(var);
      this.updateVarCount(var);
      this.current.addChild(vardec);
      this.vardecs.put(var, vardec);
      this.checkEnd(node);
   }

   public void transformConst(AConstCommand node) {
      this.checkStart(node);
      Const theconst = (Const) this.stack.get(1);
      AConst constdec = new AConst(theconst);
      this.current.addChild(constdec);
      this.checkEnd(node);
   }

   public void transformLogii(ALogiiCommand node) {
      this.checkStart(node);
      if (!this.current.hasChildren() && AIf.class.isInstance(this.current)
            && AIf.class.isInstance(this.current.parent())) {
         AIf right = (AIf) this.current;
         AIf left = (AIf) this.current.parent();
         AConditionalExp conexp = new AConditionalExp(left.condition(), right.condition(), NodeUtils.getOp(node));
         conexp.stackentry(this.stack.get(1));
         this.current = (ScriptRootNode) this.current.parent();
         ((AIf) this.current).condition(conexp);
         this.current.removeLastChild();
      } else {
         AExpression right = this.removeLastExp(false);
         if (!this.current.hasChildren() && AIf.class.isInstance(this.current)) {
            AExpression left = ((AIf) this.current).condition();
            AConditionalExp conexp = new AConditionalExp(left, right, NodeUtils.getOp(node));
            conexp.stackentry(this.stack.get(1));
            ((AIf) this.current).condition(conexp);
         } else if (!this.current.hasChildren() && AWhileLoop.class.isInstance(this.current)) {
            AExpression left = ((AWhileLoop) this.current).condition();
            AConditionalExp conexp = new AConditionalExp(left, right, NodeUtils.getOp(node));
            conexp.stackentry(this.stack.get(1));
            ((AWhileLoop) this.current).condition(conexp);
         } else {
            AExpression left = this.removeLastExp(false);
            AConditionalExp conexp = new AConditionalExp(left, right, NodeUtils.getOp(node));
            conexp.stackentry(this.stack.get(1));
            this.current.addChild(conexp);
         }
      }

      this.checkEnd(node);
   }

   public void transformBinary(ABinaryCommand node) {
      this.checkStart(node);
      AExpression right = this.removeLastExp(false);
      AExpression left = this.removeLastExp(this.state == 4);
      AExpression exp;
      if (NodeUtils.isArithmeticOp(node)) {
         exp = new ABinaryExp(left, right, NodeUtils.getOp(node));
      } else {
         if (!NodeUtils.isConditionalOp(node)) {
            throw new RuntimeException("Unknown binary op at " + this.nodedata.getPos(node));
         }

         exp = new AConditionalExp(left, right, NodeUtils.getOp(node));
      }

      exp.stackentry(this.stack.get(1));
      this.current.addChild((ScriptNode) exp);
      this.checkEnd(node);
   }

   public void transformUnary(AUnaryCommand node) {
      this.checkStart(node);
      AExpression exp = this.removeLastExp(false);
      AUnaryExp unexp = new AUnaryExp(exp, NodeUtils.getOp(node));
      unexp.stackentry(this.stack.get(1));
      this.current.addChild(unexp);
      this.checkEnd(node);
   }

   public void transformStack(AStackCommand node) {
      this.checkStart(node);
      ScriptNode last = this.current.getLastChild();
      AVarRef varref = this.getVarToAssignTo(node);
      boolean prefix;
      if (AVarRef.class.isInstance(last) && ((AVarRef) last).var() == varref.var()) {
         this.removeLastExp(true);
         prefix = false;
      } else {
         this.state = 5;
         prefix = true;
      }

      AUnaryModExp unexp = new AUnaryModExp(varref, NodeUtils.getOp(node), prefix);
      unexp.stackentry(this.stack.get(1));
      this.current.addChild(unexp);
      this.checkEnd(node);
   }

   public void transformDestruct(ADestructCommand node) {
      this.checkStart(node);
      this.updateStructVar(node);
      this.checkEnd(node);
   }

   public void transformBp(ABpCommand node) {
      this.checkStart(node);
      this.checkEnd(node);
   }

   public void transformStoreState(AStoreStateCommand node) {
      this.checkStart(node);
      this.state = 2;
      this.checkEnd(node);
   }

   public void transformDeadCode(Node node) {
      this.checkEnd(node);
   }

   public boolean atLastCommand(Node node) {
      if (this.nodedata.getPos(node) == this.current.getEnd()) {
         return true;
      } else if (ASwitchCase.class.isInstance(this.current)
            && ((ASwitch) ((ASwitchCase) this.current).parent()).end() == this.nodedata.getPos(node)) {
         return true;
      } else {
         if (ASub.class.isInstance(this.current)) {
            Node next = NodeUtils.getNextCommand(node, this.nodedata);
            if (next == null) {
               return true;
            }
         }

         if (AIf.class.isInstance(this.current) || AElse.class.isInstance(this.current)) {
            Node next = NodeUtils.getNextCommand(node, this.nodedata);
            if (this.nodedata.getPos(next) == this.current.getEnd()) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isMiddleOfReturn(Node node) {
      if (!this.root.type().equals((byte) 0) && this.current.hasChildren()
            && AReturnStatement.class.isInstance(this.current.getLastChild())) {
         return true;
      } else {
         if (this.root.type().equals((byte) 0)) {
            Node next = NodeUtils.getNextCommand(node, this.nodedata);
            if (next != null && AJumpCommand.class.isInstance(next)
                  && AReturn.class.isInstance(this.nodedata.getDestination(next))) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean currentContainsVars(List<Variable> vars) {
      for (Variable var : vars) {
         if (!var.isParam()) {
            AVarDecl vardec = this.vardecs.get(var);
            if (vardec != null) {
               ScriptNode parent = vardec.parent();
               boolean found = false;

               while (parent != null && !found) {
                  if (parent == this.current) {
                     found = true;
                  } else {
                     parent = parent.parent();
                  }
               }

               if (!found) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private int getEarlierDec(AVarDecl vardec, int earliestdec) {
      if (this.current.getChildLocation(vardec) == -1) {
         return -1;
      } else if (earliestdec == -1) {
         return this.current.getChildLocation(vardec);
      } else {
         return this.current.getChildLocation(vardec) < earliestdec ? this.current.getChildLocation(vardec)
               : earliestdec;
      }
   }

   public AExpression getReturnExp() {
      ScriptNode last = this.current.removeLastChild();
      if (AModifyExp.class.isInstance(last)) {
         return ((AModifyExp) last).expression();
      } else if (AExpressionStatement.class.isInstance(last)
            && AModifyExp.class.isInstance(((AExpressionStatement) last).exp())) {
         return ((AModifyExp) ((AExpressionStatement) last).exp()).expression();
      } else if (AReturnStatement.class.isInstance(last)) {
         return ((AReturnStatement) last).exp();
      } else {
         System.out.println(last);
         throw new RuntimeException("Trying to get return expression, unexpected scriptnode class " + last.getClass());
      }
   }

   private void checkSwitchEnd(AMoveSpCommand node) {
      if (ASwitchCase.class.isInstance(this.current)) {
         StackEntry entry = this.stack.get(1);
         if (Variable.class.isInstance(entry)
               && ((ASwitch) this.current.parent()).switchExp().stackentry().equals(entry)) {
            ((ASwitch) this.current.parent()).end(this.nodedata.getPos(node));
            this.updateSwitchUnknowns((ASwitch) this.current.parent());
         }
      }
   }

   private void updateSwitchUnknowns(ASwitch aswitch) {
      ASwitchCase acase = null;

      while ((acase = aswitch.getNextCase(acase)) != null) {
         for (AUnkLoopControl unk : acase.getUnknowns()) {
            if (unk.getDestination() > aswitch.end()) {
               acase.replaceUnknown(unk, new AContinueStatement());
            } else {
               acase.replaceUnknown(unk, new ABreakStatement());
            }
         }
      }
   }

   private ScriptRootNode getLoop() {
      return this.getEnclosingLoop(this.current);
   }

   private ScriptRootNode getEnclosingLoop(ScriptNode start) {
      for (ScriptNode node = start; node != null; node = node.parent()) {
         if (ADoLoop.class.isInstance(node) || AWhileLoop.class.isInstance(node)) {
            return (ScriptRootNode) node;
         }
      }

      return null;
   }

   private ScriptRootNode getBreakable() {
      for (ScriptNode node = this.current; node != null; node = node.parent()) {
         if (ADoLoop.class.isInstance(node) || AWhileLoop.class.isInstance(node)
               || ASwitchCase.class.isInstance(node)) {
            return (ScriptRootNode) node;
         }
      }

      return null;
   }

   private AControlLoop getLoop(Node destination, Node origin) {
      Node beforeJump = NodeUtils.getPreviousCommand(origin, this.nodedata);
      return NodeUtils.isJzPastOne(beforeJump)
            ? new ADoLoop(this.nodedata.getPos(destination), this.nodedata.getPos(origin))
            : new AWhileLoop(this.nodedata.getPos(destination), this.nodedata.getPos(origin));
   }

   private AExpression removeIfAsExp() {
      AIf aif = (AIf) this.current;
      AExpression exp = aif.condition();
      this.current = (ScriptRootNode) this.current.parent();
      this.current.removeChild(aif);
      aif.parent(null);
      exp.parent(null);
      return exp;
   }

   private AExpression removeLastExp(boolean forceOneOnly) {
      if (!this.current.hasChildren() && AIf.class.isInstance(this.current)) {
         return this.removeIfAsExp();
      } else {
         ScriptNode anode = this.current.removeLastChild();
         if (!AExpression.class.isInstance(anode)) {
            if (!forceOneOnly && AVarDecl.class.isInstance(anode) && ((AVarDecl) anode).exp() != null) {
               return ((AVarDecl) anode).removeExp();
            } else {
               System.out.println(anode.toString());
               throw new RuntimeException("Last child not an expression: " + anode.getClass());
            }
         } else {
            if (!forceOneOnly
                  && AVarRef.class.isInstance(anode)
                  && !((AVarRef) anode).var().isAssigned()
                  && !((AVarRef) anode).var().isParam()
                  && this.current.hasChildren()) {
               ScriptNode last = this.current.getLastChild();
               if (AExpression.class.isInstance(last)
                     && ((AVarRef) anode).var().equals(((AExpression) last).stackentry())) {
                  return this.removeLastExp(false);
               }

               if (AVarDecl.class.isInstance(last) && ((AVarRef) anode).var().equals(((AVarDecl) last).var())
                     && ((AVarDecl) last).exp() != null) {
                  return this.removeLastExp(false);
               }
            }

            return (AExpression) anode;
         }
      }
   }

   private AExpression getLastExp() {
      ScriptNode anode = this.current.getLastChild();
      if (!AExpression.class.isInstance(anode)) {
         if (AVarDecl.class.isInstance(anode) && ((AVarDecl) anode).isFcnReturn()) {
            return ((AVarDecl) anode).exp();
         } else {
            System.out.println(anode.toString());
            throw new RuntimeException("Last child not an expression " + anode);
         }
      } else {
         return (AExpression) anode;
      }
   }

   private AExpression getPreviousExp(int pos) {
      ScriptNode node = this.current.getPreviousChild(pos);
      if (node == null) {
         return null;
      } else if (AVarDecl.class.isInstance(node) && ((AVarDecl) node).isFcnReturn()) {
         return ((AVarDecl) node).exp();
      } else {
         return !AExpression.class.isInstance(node) ? null : (AExpression) node;
      }
   }

   public void setVarStructName(VarStruct varstruct) {
      if (varstruct.name() == null) {
         int count = 1;
         Type key = new Type((byte) -15);
         Integer curcount = this.varcounts.get(key);
         if (curcount != null) {
            count += curcount;
         }

         varstruct.name(this.varprefix, count);
         this.varcounts.put(key, Integer.valueOf(count));
      }
   }

   private void updateVarCount(Variable var) {
      int count = 1;
      Type key = var.type();
      Integer curcount = this.varcounts.get(key);
      if (curcount != null) {
         count += curcount;
      }

      var.name(this.varprefix, count);
      this.varcounts.put(key, Integer.valueOf(count));
   }

   private void updateStructVar(ADestructCommand node) {
      AVarRef varref = (AVarRef) this.getLastExp();
      int removesize = NodeUtils.stackSizeToPos(node.getSizeRem());
      int savestart = NodeUtils.stackSizeToPos(node.getOffset());
      int savesize = NodeUtils.stackSizeToPos(node.getSizeSave());
      if (savesize > 1) {
         throw new RuntimeException("Ah-ha!  A nested struct!  Now I have to code for that.  *sob*");
      } else {
         this.setVarStructName((VarStruct) varref.var());
         Variable var = (Variable) this.stack.get(removesize - savestart);
         varref.chooseStructElement(var);
      }
   }

   private AVarRef getVarToAssignTo(AStackCommand node) {
      int loc = NodeUtils.stackOffsetToPos(node.getOffset());
      if (NodeUtils.isGlobalStackOp(node)) {
         loc--;
      }

      Variable var;
      if (NodeUtils.isGlobalStackOp(node)) {
         var = (Variable) this.subdata.getGlobalStack().get(loc);
      } else {
         if (!Variable.class.isInstance(this.stack.get(loc))) {
            System.out.println("not a variable at loc " + loc);
            System.out.println(this.stack);
         }

         var = (Variable) this.stack.get(loc);
      }

      var.assigned();
      return new AVarRef(var);
   }

   private boolean isReturn(ACopyDownSpCommand node) {
      return !this.root.type().equals((byte) 0) && this.stack.size() == NodeUtils.stackOffsetToPos(node.getOffset());
   }

   private boolean isReturn(AJumpCommand node) {
      Node dest = NodeUtils.getCommandChild(this.nodedata.getDestination(node));
      if (NodeUtils.isReturn(dest)) {
         return true;
      } else if (AMoveSpCommand.class.isInstance(dest)) {
         Node afterdest = NodeUtils.getNextCommand(dest, this.nodedata);
         return afterdest == null;
      } else {
         return false;
      }
   }

   private AVarRef getVarToAssignTo(ACopyDownSpCommand node) {
      return (AVarRef) this.getVar(NodeUtils.stackSizeToPos(node.getSize()),
            NodeUtils.stackOffsetToPos(node.getOffset()), this.stack, true, this);
   }

   private AVarRef getVarToAssignTo(ACopyDownBpCommand node) {
      return (AVarRef) this.getVar(
            NodeUtils.stackSizeToPos(node.getSize()),
            NodeUtils.stackOffsetToPos(node.getOffset()),
            this.subdata.getGlobalStack(),
            true,
            this.subdata.globalState());
   }

   private AExpression getVarToCopy(ACopyTopSpCommand node) {
      return this.getVar(NodeUtils.stackSizeToPos(node.getSize()), NodeUtils.stackOffsetToPos(node.getOffset()),
            this.stack, false, this);
   }

   private AExpression getVarToCopy(ACopyTopBpCommand node) {
      return this.getVar(
            NodeUtils.stackSizeToPos(node.getSize()),
            NodeUtils.stackOffsetToPos(node.getOffset()),
            this.subdata.getGlobalStack(),
            false,
            this.subdata.globalState());
   }

   private AExpression getVar(int copy, int loc, LocalVarStack stack, boolean assign, SubScriptState state) {
      boolean isstruct = copy > 1;
      StackEntry entry = stack.get(loc);
      if (!Variable.class.isInstance(entry) && assign) {
         throw new RuntimeException("Attempting to assign to a non-variable");
      } else if (Const.class.isInstance(entry)) {
         return new AConst((Const) entry);
      } else {
         Variable var = (Variable) entry;
         if (!isstruct) {
            if (assign) {
               var.assigned();
            }

            return new AVarRef(var);
         } else if (var.isStruct()) {
            if (assign) {
               var.varstruct().assigned();
            }

            state.setVarStructName(var.varstruct());
            return new AVarRef(var.varstruct());
         } else {
            VarStruct newstruct = new VarStruct();
            newstruct.addVar(var);

            for (int i = loc - 1; i > loc - copy; i--) {
               var = (Variable) stack.get(i);
               newstruct.addVar(var);
            }

            if (assign) {
               newstruct.assigned();
            }

            this.subdata.addStruct(newstruct);
            state.setVarStructName(newstruct);
            return new AVarRef(newstruct);
         }
      }
   }

   private List<AVarRef> getParams(int paramcount) {
      ArrayList<AVarRef> params = new ArrayList<>();

      for (int i = 1; i <= paramcount; i++) {
         Variable var = (Variable) this.stack.get(i);
         var.name("Param", i);
         AVarRef varref = new AVarRef(var);
         params.add(varref);
      }

      return params;
   }

   private List<AExpression> removeFcnParams(AJumpToSubroutine node) {
      ArrayList<AExpression> params = new ArrayList<>();
      int paramcount = this.subdata.getState(this.nodedata.getDestination(node)).getParamCount();
      int i = 0;

      while (i < paramcount) {
         AExpression exp = this.removeLastExp(false);
         i += this.getExpSize(exp);
         params.add(exp);
      }

      return params;
   }

   private int getExpSize(AExpression exp) {
      if (AVarRef.class.isInstance(exp)) {
         return ((AVarRef) exp).var().size();
      } else {
         return AConst.class.isInstance(exp) ? 1 : 1;
      }
   }

   private List<AExpression> removeActionParams(AActionCommand node) {
      ArrayList<AExpression> params = new ArrayList<>();
      List<Type> paramtypes = NodeUtils.getActionParamTypes(node, this.actions);
      int paramcount = NodeUtils.getActionParamCount(node);

      for (int i = 0; i < paramcount; i++) {
         Type paramtype = paramtypes.get(i);
         AExpression exp;
         if (paramtype.equals((byte) -16)) {
            exp = this.getLastExp();
            if (!exp.stackentry().type().equals((byte) -16) && !exp.stackentry().type().equals((byte) -15)) {
               exp = new AVectorConstExp(
                     this.removeLastExp(false),
                     this.removeLastExp(false),
                     this.removeLastExp(false));
            } else {
               exp = this.removeLastExp(false);
            }
         } else {
            exp = this.removeLastExp(false);
         }

         params.add(exp);
      }

      return params;
   }

   private byte getFcnId(AJumpToSubroutine node) {
      return this.subdata.getState(this.nodedata.getDestination(node)).getId();
   }

   private Type getFcnType(AJumpToSubroutine node) {
      return this.subdata.getState(this.nodedata.getDestination(node)).type();
   }

   private int getNextCommand(AJumpCommand node) {
      return this.nodedata.getPos(node) + 6;
   }

   private int getPriorToDestCommand(AJumpCommand node) {
      return this.nodedata.getPos(this.nodedata.getDestination(node)) - 2;
   }
}

