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
import com.kotor.resource.formats.ncs.scriptnode.AErrorComment;
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
import com.kotor.resource.formats.ncs.stack.IntConst;
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
   private boolean preferSwitches;

   public SubScriptState(NodeAnalysisData nodedata, SubroutineAnalysisData subdata, LocalVarStack stack,
         SubroutineState protostate, ActionsData actions, boolean preferSwitches) {
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
      this.preferSwitches = preferSwitches;
   }

   public SubScriptState(NodeAnalysisData nodedata, SubroutineAnalysisData subdata, LocalVarStack stack, boolean preferSwitches) {
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
      this.preferSwitches = preferSwitches;
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
      // #region agent log
      try {
         java.nio.file.Files.write(java.nio.file.Paths.get("g:\\GitHub\\HoloPatcher.NET\\vendor\\DeNCS\\.cursor\\debug.log"),
            (java.nio.charset.StandardCharsets.UTF_8.encode("{\"id\":\"log_" + System.currentTimeMillis() + "_getvars\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"SubScriptState.java:222\",\"message\":\"getVariables called\",\"data\":{\"subroutine\":\"" + (this.root != null ? this.root.name() : "null") + "\",\"vardecsSize\":" + (this.vardecs != null ? this.vardecs.size() : 0) + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n").array()),
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
      } catch (Exception e) {}
      // #endregion
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
      java.util.ArrayList<Variable> paramVars = this.root.getParamVars();
      // #region agent log
      try {
         int placeholderCount = 0;
         if (paramVars != null) {
            for (Variable v : paramVars) {
               if (v.toString().contains("__unknown_param_")) placeholderCount++;
            }
         }
         java.nio.file.Files.write(java.nio.file.Paths.get("g:\\GitHub\\HoloPatcher.NET\\vendor\\DeNCS\\.cursor\\debug.log"),
            (java.nio.charset.StandardCharsets.UTF_8.encode("{\"id\":\"log_" + System.currentTimeMillis() + "_params\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"SubScriptState.java:236\",\"message\":\"paramVars retrieved\",\"data\":{\"paramCount\":" + (paramVars != null ? paramVars.size() : 0) + ",\"placeholderCount\":" + placeholderCount + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n").array()),
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
      } catch (Exception e) {}
      // #endregion
      vars.addAll(paramVars);
      // #region agent log
      try {
         int totalPlaceholders = 0;
         for (Variable v : vars) {
            if (v.toString().contains("__unknown_param_")) totalPlaceholders++;
         }
         java.nio.file.Files.write(java.nio.file.Paths.get("g:\\GitHub\\HoloPatcher.NET\\vendor\\DeNCS\\.cursor\\debug.log"),
            (java.nio.charset.StandardCharsets.UTF_8.encode("{\"id\":\"log_" + System.currentTimeMillis() + "_final\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"SubScriptState.java:237\",\"message\":\"getVariables returning\",\"data\":{\"totalVars\":" + vars.size() + ",\"totalPlaceholders\":" + totalPlaceholders + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n").array()),
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
      } catch (Exception e) {}
      // #endregion
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
      int nodePos = this.nodedata.getPos(node);
      System.err.println("DEBUG checkStart: pos=" + nodePos + ", current=" + this.current.getClass().getSimpleName() +
            ", hasChildren=" + this.current.hasChildren());

      if (this.current.hasChildren()) {
         ScriptNode lastNode = this.current.getLastChild();
         System.err.println("DEBUG checkStart: lastChild=" + (lastNode != null ? lastNode.getClass().getSimpleName() : "null"));

         if (ASwitch.class.isInstance(lastNode)
               && this.nodedata.getPos(node) == ((ASwitch) lastNode).getFirstCaseStart()) {
            int firstCaseStart = ((ASwitch) lastNode).getFirstCaseStart();
            System.err.println("DEBUG checkStart: entering first switch case (firstCaseStart=" + firstCaseStart + ")");
            this.current = ((ASwitch) lastNode).getFirstCase();
         } else if (ASwitch.class.isInstance(lastNode)) {
            int firstCaseStart = ((ASwitch) lastNode).getFirstCaseStart();
            System.err.println("DEBUG checkStart: lastChild is ASwitch but nodePos (" + nodePos + ") != firstCaseStart (" + firstCaseStart + ")");
         }
      }

      ScriptNode lastNode = null;
   }

   private void checkEnd(Node node) {
      int nodePos = this.nodedata.getPos(node);
      System.err.println("DEBUG checkEnd: START pos=" + nodePos + ", current=" + this.current.getClass().getSimpleName() +
            ", currentEnd=" + this.current.getEnd() + ", state=" + this.state);

      while (this.current != null) {
         if (this.nodedata.getPos(node) != this.current.getEnd()) {
            System.err.println("DEBUG checkEnd: nodePos != currentEnd, returning early");
            return;
         }

         System.err.println("DEBUG checkEnd: nodePos == currentEnd, processing " + this.current.getClass().getSimpleName());

         if (ASwitchCase.class.isInstance(this.current)) {
            System.err.println("DEBUG checkEnd: current is ASwitchCase");
            ASwitchCase nextCase = ((ASwitch) this.current.parent()).getNextCase((ASwitchCase) this.current);
            if (nextCase != null) {
               System.err.println("DEBUG checkEnd: moving to next case");
               this.current = nextCase;
            } else {
               ScriptRootNode newCurrent = (ScriptRootNode) this.current.parent().parent();
               System.err.println("DEBUG checkEnd: no next case, moving to " + (newCurrent != null ? newCurrent.getClass().getSimpleName() : "null"));
               this.current = newCurrent;
            }

            nextCase = null;
            return;
         }

         if (AIf.class.isInstance(this.current)) {
            System.err.println("DEBUG checkEnd: current is AIf, checking for else block");
            Node dest = this.nodedata.getDestination(node);
            if (dest == null) {
               System.err.println("DEBUG checkEnd: dest is null, returning");
               return;
            }

            // Get the destination position and AIf end position
            int destPos = this.nodedata.getPos(dest);
            int aifEnd = this.current.getEnd();
            System.err.println("DEBUG checkEnd: AIf end=" + aifEnd + ", destPos=" + destPos + ", expectedElseStart=" + (aifEnd + 6));

            // If the destination is exactly 6 bytes after the AIf's end, there's no else block
            // Otherwise, there's an else block starting at AIf's end + 6
            if (destPos != aifEnd + 6) {
               System.err.println("DEBUG checkEnd: destPos != aifEnd+6, creating else block");
               // Check if this AIf is inside an AElse (else-if chain)
               // If so, create the next AElse as a sibling of the parent AElse, not nested
               ScriptRootNode parent = (ScriptRootNode) this.current.parent();
               System.err.println("DEBUG checkEnd: AIf parent=" + (parent != null ? parent.getClass().getSimpleName() : "null"));

               // Safety check: don't create AElse if AIf is at root level (shouldn't happen, but protect against it)
               if (parent == null || ASub.class.isInstance(this.current)) {
                  System.err.println("DEBUG checkEnd: AIf at root level, not creating else");
                  dest = null;
                  return;
               }

               ScriptRootNode elseParent = parent;

               // Check if this AIf is inside an AElse (else-if chain case)
               // If the parent is an AElse and the AIf is the last/only child,
               // the next AElse should be a sibling of the parent AElse, not nested
               if (AElse.class.isInstance(parent)) {
                  System.err.println("DEBUG checkEnd: AIf is inside AElse, checking else-if chain");
                  // Check if this AIf is the last child of the AElse
                  boolean isLastChild = parent.hasChildren() && parent.getLastChild() == this.current;
                  System.err.println("DEBUG checkEnd: isLastChild=" + isLastChild + ", parentEnd=" + parent.getEnd());

                  if (isLastChild) {
                     // Check if this is a pure else-if chain (AIf's else ends at parent AElse's end)
                     // or a nested if-else (there's more content in parent AElse after this AIf's else)
                     //
                     // For pure else-if: destPos is at or past parent's end → use grandParent (siblings)
                     // For nested: destPos is before parent's end → use parent (keep nested)
                     int parentEnd = parent.getEnd();

                     // If the JMP destination is at or past the parent AElse's end,
                     // this is a continuation of the else-if chain at the same level
                     if (destPos >= parentEnd) {
                        ScriptRootNode grandParent = (ScriptRootNode) parent.parent();
                        System.err.println("DEBUG checkEnd: destPos >= parentEnd, using grandParent=" +
                              (grandParent != null ? grandParent.getClass().getSimpleName() : "null"));
                        if (grandParent != null) {
                           elseParent = grandParent;
                        }
                     } else {
                        System.err.println("DEBUG checkEnd: destPos < parentEnd, keeping nested (elseParent=parent)");
                     }
                     // Otherwise, the else block is nested inside the parent AElse (has siblings)
                     // Keep elseParent = parent
                  } else {
                     System.err.println("DEBUG checkEnd: AIf not last child, keeping nested");
                  }
                  // If the AIf is not the last child, keep parent as elseParent (nested else)
               } else {
                  System.err.println("DEBUG checkEnd: AIf parent is not AElse, using parent as elseParent");
               }

               // Safety check: ensure elseParent is not null and is not the AIf itself
               if (elseParent == null || elseParent == this.current) {
                  System.err.println("DEBUG checkEnd: elseParent invalid, using root");
                  elseParent = this.root;
               }

               int elseStart = this.current.getEnd() + 6;
               int elseEnd = this.nodedata.getPos(NodeUtils.getPreviousCommand(dest, this.nodedata));
               System.err.println("DEBUG checkEnd: creating AElse start=" + elseStart + ", end=" + elseEnd +
                     ", elseParent=" + elseParent.getClass().getSimpleName());

               AElse aelse = new AElse(elseStart, elseEnd);
               this.current = elseParent;
               this.current.addChild(aelse);
               this.current = aelse;
               aelse = null;
               dest = null;
               return;
            } else {
               System.err.println("DEBUG checkEnd: destPos == aifEnd+6, no else block");
            }
         }

         if (ADoLoop.class.isInstance(this.current)) {
            System.err.println("DEBUG checkEnd: current is ADoLoop, calling transformEndDoLoop");
            this.transformEndDoLoop();
         }

         ScriptRootNode newCurrent = (ScriptRootNode) this.current.parent();
         System.err.println("DEBUG checkEnd: moving up to parent=" + (newCurrent != null ? newCurrent.getClass().getSimpleName() : "null"));
         this.current = newCurrent;
      }

      System.err.println("DEBUG checkEnd: END, setting state=-1");
      this.state = -1;
   }

   public boolean inActionArg() {
      return this.state == 2;
   }

   public void transformPlaceholderVariableRemoved(Variable var) {
      AVarDecl vardec = this.vardecs.get(var);
      if (vardec != null && vardec.isFcnReturn()) {
         AExpression exp = vardec.exp();
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
      AExpression exp = null;
   }

   public void emitError(Node node, int pos) {
      String message = "ERROR: failed to decompile statement";
      if (pos >= 0) {
         message = message + " at " + pos;
      }

      this.current.addChild(new AErrorComment(message));
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
         // Equality comparison - prefer switch when preferSwitches is enabled
         if (this.state != 4) {
            AConditionalExp cond = (AConditionalExp) this.removeLastExp(true);
            // When preferSwitches is enabled, be more aggressive about creating switches
            // Check if we can add to an existing switch or create a new one
            boolean canCreateSwitch = AConst.class.isInstance(cond.right());
            ASwitch existingSwitch = null;

            // Check if we can continue an existing switch when preferSwitches is enabled
            if (this.preferSwitches && this.current.hasChildren()) {
               ScriptNode last = this.current.getLastChild();
               if (ASwitch.class.isInstance(last)) {
                  existingSwitch = (ASwitch) last;
                  // Verify the switch expression matches
                  if (AVarRef.class.isInstance(cond.left()) && AVarRef.class.isInstance(existingSwitch.switchExp())
                        && ((AVarRef) cond.left()).var().equals(((AVarRef) existingSwitch.switchExp()).var())) {
                     // Can continue existing switch
                     ASwitchCase aprevcase = existingSwitch.getLastCase();
                     if (aprevcase != null) {
                        aprevcase.end(this.nodedata
                              .getPos(NodeUtils.getPreviousCommand(this.nodedata.getDestination(node), this.nodedata)));
                     }
                     ASwitchCase acasex = new ASwitchCase(this.nodedata.getPos(this.nodedata.getDestination(node)),
                           (AConst) cond.right());
                     existingSwitch.addCase(acasex);
                     this.state = 4;
                     this.checkEnd(node);
                     return;
                  }
               }
            }

            if (canCreateSwitch) {
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
               // Fall back to if statement if we can't create a switch
               AIf aif = new AIf(this.nodedata.getPos(node), this.nodedata.getPos(this.nodedata.getDestination(node)) - 6,
                     cond);
               this.current.addChild(aif);
               this.current = aif;
            }
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
      } else if (AIf.class.isInstance(this.current) && this.isModifyConditional() && this.state != 4) {
         // Don't modify AIf's end when processing switch cases (state == 4)
         int newEnd = this.nodedata.getPos(this.nodedata.getDestination(node)) - 6;
         System.err.println("DEBUG transformJZ: modifying AIf end (isModifyConditional) from " +
               this.current.getEnd() + " to " + newEnd);
         ((AIf) this.current).end(newEnd);
         if (this.current.hasChildren()) {
            this.current.removeLastChild();
         }
      } else if (AIf.class.isInstance(this.current) && this.isModifyConditional() && this.state == 4) {
         System.err.println("DEBUG transformJZ: NOT modifying AIf end (state==4, processing switch case)");
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

      // Ensure AIf's end is up-to-date before checkEnd, in case it needs to check for else blocks
      // The end might have been set in the constructor, but we should verify it's correct
      // BUT: Don't update AIf's end when we're processing switch cases (state == 4), as those
      // JZ instructions are for switch case checks, not the original if condition
      if (AIf.class.isInstance(this.current) && this.state != 4) {
         Node destNode = this.nodedata.getDestination(node);
         if (destNode != null) {
            int expectedEnd = this.nodedata.getPos(destNode) - 6;
            int currentEnd = this.current.getEnd();
            System.err.println("DEBUG transformJZ: AIf end check - currentEnd=" + currentEnd +
                  ", expectedEnd=" + expectedEnd + ", nodePos=" + this.nodedata.getPos(node));
            if (currentEnd != expectedEnd) {
               System.err.println("DEBUG transformJZ: UPDATING AIf end from " + currentEnd + " to " + expectedEnd);
               ((AIf) this.current).end(expectedEnd);
            } else {
               System.err.println("DEBUG transformJZ: AIf end already correct, not updating");
            }
         }
      } else if (AIf.class.isInstance(this.current) && this.state == 4) {
         System.err.println("DEBUG transformJZ: AIf end NOT updated (state==4, processing switch case)");
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
      int nodePos = this.nodedata.getPos(node);
      int destPos = dest != null ? this.nodedata.getPos(dest) : -1;

      System.err.println("DEBUG transformJump: pos=" + nodePos + " (0x" + Integer.toHexString(nodePos) + "), destPos=" + destPos +
            ", state=" + this.state + ", current=" + this.current.getClass().getSimpleName() +
            ", currentEnd=" + this.current.getEnd() + ", destType=" + (dest != null ? dest.getClass().getSimpleName() : "null"));

      if (this.state == 2) {
         System.err.println("DEBUG transformJump: state==2, creating AActionArgExp");
         this.state = 0;
         AActionArgExp aarg = new AActionArgExp(this.getNextCommand(node), this.getPriorToDestCommand(node));
         this.current.addChild(aarg);
         this.current = aarg;
      } else {
         boolean atIfEnd = this.isAtIfEnd(node);
         System.err.println("DEBUG transformJump: isAtIfEnd=" + atIfEnd);

         if (!atIfEnd) {
            // Only process as return/break/continue if we're NOT at the end of an enclosing AIf
            // (otherwise, this JMP is the "skip else" jump and should be handled by checkEnd)
            if (this.state == 4) {
               System.err.println("DEBUG transformJump: state==4 (switch), handling switch case/end");
               ASwitch aswitch = (ASwitch) this.current.getLastChild();
               ASwitchCase aprevcase = aswitch.getLastCase();
               if (aprevcase != null) {
                  int prevCaseEnd = this.nodedata.getPos(NodeUtils.getPreviousCommand(dest, this.nodedata));
                  System.err.println("DEBUG transformJump: setting prevCase end to " + prevCaseEnd);
                  aprevcase.end(prevCaseEnd);
               }

               if (AMoveSpCommand.class.isInstance(dest)) {
                  int switchEnd = this.nodedata.getPos(this.nodedata.getDestination(node));
                  System.err.println("DEBUG transformJump: dest is MoveSpCommand, setting switch end to " + switchEnd);
                  aswitch.end(switchEnd);
               } else {
                  int defaultStart = this.nodedata.getPos(dest);
                  System.err.println("DEBUG transformJump: creating default case at " + defaultStart);
                  ASwitchCase adefault = new ASwitchCase(defaultStart);
                  aswitch.addDefaultCase(adefault);
               }

               this.state = 0;
            } else {
               boolean isRet = this.isReturn(node);
               System.err.println("DEBUG transformJump: isReturn=" + isRet);

               if (isRet) {
                  System.err.println("DEBUG transformJump: treating as RETURN, adding AReturnStatement to " + this.current.getClass().getSimpleName());
                  AReturnStatement areturn;
                  if (!this.root.type().equals((byte) 0)) {
                     areturn = new AReturnStatement(this.getReturnExp());
                  } else {
                     areturn = new AReturnStatement();
                  }

                  this.current.addChild(areturn);
               } else if (destPos >= nodePos) {
                  System.err.println("DEBUG transformJump: forward jump (destPos >= nodePos), checking for break/continue");
                  ScriptRootNode loop = this.getBreakable();
                  System.err.println("DEBUG transformJump: breakable=" + (loop != null ? loop.getClass().getSimpleName() : "null"));

                  if (ASwitchCase.class.isInstance(loop)) {
                     loop = this.getEnclosingLoop(loop);
                     System.err.println("DEBUG transformJump: enclosingLoop=" + (loop != null ? loop.getClass().getSimpleName() : "null"));
                     if (loop == null) {
                        System.err.println("DEBUG transformJump: adding ABreakStatement (no enclosing loop)");
                        ABreakStatement abreak = new ABreakStatement();
                        this.current.addChild(abreak);
                     } else {
                        System.err.println("DEBUG transformJump: adding AUnkLoopControl");
                        AUnkLoopControl aunk = new AUnkLoopControl(destPos);
                        this.current.addChild(aunk);
                     }
                  } else if (loop != null && destPos > loop.getEnd()) {
                     System.err.println("DEBUG transformJump: adding ABreakStatement (destPos > loop.end)");
                     ABreakStatement abreak = new ABreakStatement();
                     this.current.addChild(abreak);
                  } else {
                     loop = this.getLoop();
                     System.err.println("DEBUG transformJump: getLoop()=" + (loop != null ? loop.getClass().getSimpleName() : "null"));
                     if (loop != null && destPos <= loop.getEnd()) {
                        System.err.println("DEBUG transformJump: adding AContinueStatement");
                        AContinueStatement acont = new AContinueStatement();
                        this.current.addChild(acont);
                     }
                  }
               } else {
                  System.err.println("DEBUG transformJump: backward jump, no action taken");
               }
            }
         } else {
            System.err.println("DEBUG transformJump: at if end, skipping return/break/continue handling (will be handled by checkEnd)");
         }
      }

      System.err.println("DEBUG transformJump: calling checkEnd, current=" + this.current.getClass().getSimpleName());
      this.checkEnd(node);
   }

   public void transformJSR(AJumpToSubroutine node) {
      this.checkStart(node);
      AFcnCallExp jsr = new AFcnCallExp(this.getFcnId(node), this.removeFcnParams(node));
      if (!this.getFcnType(node).equals((byte) 0)) {
         // Ensure there's a decl to attach; if none, create a placeholder
         Variable retVar = this.stack.size() >= 1 ? (Variable) this.stack.get(1) : new Variable(new Type((byte)0));
         AVarDecl decl;
         // Check if variable is already declared to prevent duplicates
         decl = this.vardecs.get(retVar);
         if (decl == null) {
            // Also check if last child is a matching AVarDecl
            if (this.current.hasChildren() && AVarDecl.class.isInstance(this.current.getLastChild())) {
               AVarDecl lastDecl = (AVarDecl) this.current.getLastChild();
               if (lastDecl.var() == retVar) {
                  decl = lastDecl;
                  this.vardecs.put(retVar, decl);
               }
            }
            if (decl == null) {
               decl = new AVarDecl(retVar);
               this.updateVarCount(retVar);
               this.current.addChild(decl);
               this.vardecs.put(retVar, decl);
            }
         }
         decl.isFcnReturn(true);
         decl.initializeExp(jsr);
         jsr.stackentry(retVar);
      } else {
         this.current.addChild(jsr);
      }

      this.checkEnd(node);
   }

   public void transformAction(AActionCommand node) {
      this.checkStart(node);
      List<AExpression> params = this.removeActionParams(node);
      String actionName;
      try {
         actionName = NodeUtils.getActionName(node, this.actions);
      } catch (RuntimeException e) {
         // Action metadata missing - use placeholder name
         actionName = "UnknownAction" + NodeUtils.getActionId(node);
      }
      AActionExp act = new AActionExp(actionName, NodeUtils.getActionId(node), params, this.actions);
      Type type;
      try {
         type = NodeUtils.getReturnType(node, this.actions);
      } catch (RuntimeException e) {
         // Action metadata missing or invalid - assume void return
         type = new Type((byte) 0);
      }
      if (!type.equals((byte) 0)) {
         Variable var = (Variable) this.stack.get(1);
         if (type.equals((byte) -16)) {
            var = var.varstruct();
         }

         act.stackentry(var);
         // Check if variable is already declared to prevent duplicates
         AVarDecl vardec = this.vardecs.get(var);
         if (vardec == null) {
            vardec = new AVarDecl(var);
            this.updateVarCount(var);
            this.current.addChild(vardec);
            this.vardecs.put(var, vardec);
         }
         vardec.isFcnReturn(true);
         vardec.initializeExp(act);
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
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());
         StackEntry sourceEntry = this.stack.get(loc);

         // For constants: when copying a constant that's already the last child,
         // we're likely doing short-circuit evaluation (e.g., for || or &&).
         // In this case, don't add a duplicate child - the duplicate is only
         // needed for stack simulation (the JZ/JNZ check), not for AST building.
         // The LOGORII/LOGANDII will consume the original constant from children,
         // leaving the correct expression (e.g., function result) for EQUAL.
         if (Const.class.isInstance(sourceEntry) && this.current.hasChildren()) {
            ScriptNode last = this.current.getLastChild();
            if (AConst.class.isInstance(last)) {
               AConst lastConst = (AConst) last;
               // Check if we're copying the exact same Const object
               // stackentry() returns the Const for AConst nodes
               if (lastConst.stackentry() == sourceEntry) {
                  // Short-circuit pattern: don't add duplicate to children
                  this.checkEnd(node);
                  return;
               }
            }
         }

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
            AExpression expr = null;
            if (AModifyExp.class.isInstance(last)) {
               expr = (AModifyExp) this.removeLastExp(true);
            } else if (AVarDecl.class.isInstance(last) && ((AVarDecl) last).isFcnReturn() && ((AVarDecl) last).exp() != null) {
               // Function return value - extract the expression and convert to statement
               // However, don't extract function calls (AActionExp) as standalone statements
               // when in assignment context, as they're almost always part of a larger expression
               // (e.g., GetGlobalNumber("X") == value, or function calls in binary operations).
               AExpression funcExp = ((AVarDecl) last).exp();
               if (AActionExp.class.isInstance(funcExp)) {
                  // Don't extract function calls as statements in assignment context
                  // They're almost always part of a larger expression being built
                  // Leave the AVarDecl in place - it will be used by EQUAL/other operations
                  // NEVER extract function calls as statements when state == 1 (assignment context)
                  expr = null; // Don't extract as statement
               } else {
                  // Non-function-call expressions can be extracted
                  expr = ((AVarDecl) last).removeExp();
                  this.current.removeLastChild(); // Remove the AVarDecl
               }
            } else if (AUnaryModExp.class.isInstance(last) || AExpression.class.isInstance(last)) {
               // Gracefully handle postfix/prefix inc/dec and other loose expressions.
               // However, don't extract function calls (AActionExp) as standalone statements
               // when in assignment context, as they're almost always part of a larger expression
               // (e.g., GetGlobalNumber("X") == value, or function calls in binary operations).
               // In assignment context, function calls should remain as part of the expression tree
               // until the full expression is built (e.g., by EQUAL, ADD, etc. operations).
               expr = (AExpression) this.removeLastExp(true);
               // Don't extract function calls as statements in assignment context
               // They're almost always part of a larger expression being built.
               // In assignment context (state == 1), function calls should remain as part of the expression tree
               // until the full expression is built (e.g., by EQUAL, ADD, etc. operations).
               if (AActionExp.class.isInstance(expr)) {
                  // Put the function call back - it's part of a larger expression
                  // Function calls in assignment context are almost never standalone statements
                  this.current.addChild((ScriptNode) expr);
                  expr = null; // Don't extract as statement
               }
            } else {
               System.out.println("uh-oh... not a modify exp at " + this.nodedata.getPos(node) + ", " + last);
            }

            if (expr != null) {
               AExpressionStatement stmt = new AExpressionStatement(expr);
               this.current.addChild(stmt);
               stmt.parent(this.current);
            }
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
      // Check if variable is already declared to prevent duplicates
      AVarDecl existingVardec = this.vardecs.get(var);
      if (existingVardec == null) {
         AVarDecl vardec = new AVarDecl(var);
         this.updateVarCount(var);
         this.current.addChild(vardec);
         this.vardecs.put(var, vardec);
      }
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

   /**
    * Checks if the current node position is at the end of any enclosing AIf.
    * This is used to detect "skip else" jumps that should not be treated as returns.
    */
   private boolean isAtIfEnd(Node node) {
      int nodePos = this.nodedata.getPos(node);

      // Debug output
      System.err.println("DEBUG isAtIfEnd: nodePos=" + nodePos + ", current=" + this.current.getClass().getSimpleName() + ", currentEnd=" + this.current.getEnd());

      // Check if current is an AIf and we're at its end
      if (AIf.class.isInstance(this.current) && nodePos == this.current.getEnd()) {
         System.err.println("DEBUG isAtIfEnd: returning true (current is AIf)");
         return true;
      }

      // Check if we're inside a switch case and the enclosing if ends here
      if (ASwitchCase.class.isInstance(this.current)) {
         ScriptNode switchNode = this.current.parent();
         System.err.println("DEBUG isAtIfEnd: in switch case, switchNode=" + (switchNode != null ? switchNode.getClass().getSimpleName() : "null"));
         if (ASwitch.class.isInstance(switchNode)) {
            ScriptNode switchParent = switchNode.parent();
            System.err.println("DEBUG isAtIfEnd: switchParent=" + (switchParent != null ? switchParent.getClass().getSimpleName() : "null"));
            if (AIf.class.isInstance(switchParent) && switchParent instanceof ScriptRootNode) {
               int parentEnd = ((ScriptRootNode) switchParent).getEnd();
               System.err.println("DEBUG isAtIfEnd: parentEnd=" + parentEnd);
               if (nodePos == parentEnd) {
                  System.err.println("DEBUG isAtIfEnd: returning true (switch in AIf)");
                  return true;
               }
            }
         }
      }

      // Walk up the parent chain to find any enclosing AIf at whose end we are
      ScriptRootNode curr = this.current;
      while (curr != null && !ASub.class.isInstance(curr)) {
         if (AIf.class.isInstance(curr) && nodePos == curr.getEnd()) {
            System.err.println("DEBUG isAtIfEnd: returning true (found AIf in parent chain)");
            return true;
         }
         ScriptNode parent = curr.parent();
         curr = parent instanceof ScriptRootNode ? (ScriptRootNode) parent : null;
      }

      System.err.println("DEBUG isAtIfEnd: returning false");
      return false;
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
            if (next != null && this.nodedata.getPos(next) == this.current.getEnd()) {
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
      if (!this.current.hasChildren()) {
         return new AConst(new IntConst(0L));
      }

      ScriptNode last = this.current.removeLastChild();
      if (AModifyExp.class.isInstance(last)) {
         return ((AModifyExp) last).expression();
      } else if (AExpressionStatement.class.isInstance(last)
            && AModifyExp.class.isInstance(((AExpressionStatement) last).exp())) {
         return ((AModifyExp) ((AExpressionStatement) last).exp()).expression();
      } else if (AReturnStatement.class.isInstance(last)) {
         return ((AReturnStatement) last).exp();
      } else if (AExpression.class.isInstance(last)) {
         return (AExpression) last;
      } else {
         // Keep decompilation alive; emit placeholder when structure is unexpected.
         return new AConst(new IntConst(0L));
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
      ArrayList<ScriptNode> trailingErrors = new ArrayList<>();
      while (this.current.hasChildren() && AErrorComment.class.isInstance(this.current.getLastChild())) {
         trailingErrors.add(this.current.removeLastChild());
      }

      if (!this.current.hasChildren() && AIf.class.isInstance(this.current)) {
         for (int i = trailingErrors.size() - 1; i >= 0; i--) {
            this.current.addChild(trailingErrors.get(i));
         }

         return this.removeIfAsExp();
      } else {
         ScriptNode anode = null;
         while (true) {
            if (!this.current.hasChildren()) {
               break;
            }
            anode = this.current.removeLastChild();
            if (AExpression.class.isInstance(anode)) {
               break;
            }
            if (AVarDecl.class.isInstance(anode)) {
               AVarDecl vardecl = (AVarDecl) anode;
               if (vardecl.isFcnReturn() && vardecl.exp() != null) {
                  // Function return value - extract the expression
                  // The AVarDecl has already been removed from children, so we just extract the expression
                  // Use removeExp() to properly clear the parent relationship and remove from AVarDecl
                  AExpression exp = vardecl.removeExp();
                  for (int i = trailingErrors.size() - 1; i >= 0; i--) {
                     this.current.addChild(trailingErrors.get(i));
                  }
                  return exp;
               } else if (!forceOneOnly && vardecl.exp() != null) {
                  // Regular variable declaration with initializer
                  AExpression exp = vardecl.removeExp();
                  for (int i = trailingErrors.size() - 1; i >= 0; i--) {
                     this.current.addChild(trailingErrors.get(i));
                  }
                  return exp;
               }
            }
            // Skip non-expression nodes and keep searching.
            anode = null;
         }

         if (anode == null) {
            return this.buildPlaceholderParam(1);
         }

         if (!forceOneOnly
               && AVarRef.class.isInstance(anode)
               && !((AVarRef) anode).var().isAssigned()
               && !((AVarRef) anode).var().isParam()
               && this.current.hasChildren()) {
            ScriptNode last = this.current.getLastChild();
            if (AExpression.class.isInstance(last)
                  && ((AVarRef) anode).var().equals(((AExpression) last).stackentry())) {
               AExpression exp = this.removeLastExp(false);
               for (int i = trailingErrors.size() - 1; i >= 0; i--) {
                  this.current.addChild(trailingErrors.get(i));
               }

               return exp;
            }

            if (AVarDecl.class.isInstance(last) && ((AVarRef) anode).var().equals(((AVarDecl) last).var())
                  && ((AVarDecl) last).exp() != null) {
               AExpression exp = this.removeLastExp(false);
               for (int i = trailingErrors.size() - 1; i >= 0; i--) {
                  this.current.addChild(trailingErrors.get(i));
               }

               return exp;
            }
         }

         for (int i = trailingErrors.size() - 1; i >= 0; i--) {
            this.current.addChild(trailingErrors.get(i));
         }

         return (AExpression) anode;
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
      Node dest = this.nodedata.getDestination(node);
      Node destChild = dest != null ? NodeUtils.getCommandChild(dest) : null;
      int nodePos = this.nodedata.getPos(node);
      int destPos = dest != null ? this.nodedata.getPos(dest) : -1;

      System.err.println("DEBUG isReturn: pos=" + nodePos + ", destPos=" + destPos +
            ", destType=" + (dest != null ? dest.getClass().getSimpleName() : "null") +
            ", destChildType=" + (destChild != null ? destChild.getClass().getSimpleName() : "null"));

      if (NodeUtils.isReturn(destChild)) {
         System.err.println("DEBUG isReturn: returning true (destChild is Return)");
         return true;
      } else if (AMoveSpCommand.class.isInstance(dest)) {
         Node afterdest = NodeUtils.getNextCommand(dest, this.nodedata);
         boolean result = afterdest == null;
         System.err.println("DEBUG isReturn: dest is MoveSpCommand, afterdest=" +
               (afterdest != null ? this.nodedata.getPos(afterdest) + " (" + afterdest.getClass().getSimpleName() + ")" : "null") +
               ", returning " + result);
         return result;
      } else {
         System.err.println("DEBUG isReturn: returning false");
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
               // Defensive check: ensure we don't access beyond stack size
               if (i < 1 || i > stack.size()) {
                  break;
               }
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
         AExpression exp;
         try {
            exp = this.removeLastExp(false);
         } catch (RuntimeException e) {
            e.printStackTrace();
            exp = this.buildPlaceholderParam(i + 1);
         }

         int expSize = this.getExpSize(exp);
         i += expSize <= 0 ? 1 : expSize;
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

   private AVarRef buildPlaceholderParam(int ordinal) {
      // #region agent log
      try {
         java.nio.file.Files.write(java.nio.file.Paths.get("g:\\GitHub\\HoloPatcher.NET\\vendor\\DeNCS\\.cursor\\debug.log"),
            (java.nio.charset.StandardCharsets.UTF_8.encode("{\"id\":\"log_" + System.currentTimeMillis() + "_" + ordinal + "\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"SubScriptState.java:1312\",\"message\":\"buildPlaceholderParam called\",\"data\":{\"ordinal\":" + ordinal + ",\"subroutine\":\"" + (this.root != null ? this.root.name() : "null") + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n").array()),
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
      } catch (Exception e) {}
      // #endregion
      Variable placeholder = new Variable(new Type((byte)-1));
      placeholder.name("__unknown_param_" + ordinal);
      placeholder.isParam(true);
      // #region agent log
      try {
         java.nio.file.Files.write(java.nio.file.Paths.get("g:\\GitHub\\HoloPatcher.NET\\vendor\\DeNCS\\.cursor\\debug.log"),
            (java.nio.charset.StandardCharsets.UTF_8.encode("{\"id\":\"log_" + System.currentTimeMillis() + "_" + ordinal + "_2\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"SubScriptState.java:1316\",\"message\":\"placeholder created\",\"data\":{\"ordinal\":" + ordinal + ",\"isParam\":" + placeholder.isParam() + ",\"type\":\"" + placeholder.type() + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n").array()),
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
      } catch (Exception e) {}
      // #endregion
      return new AVarRef(placeholder);
   }

   private List<AExpression> removeActionParams(AActionCommand node) {
      ArrayList<AExpression> params = new ArrayList<>();
      List<Type> paramtypes;
      try {
         paramtypes = NodeUtils.getActionParamTypes(node, this.actions);
      } catch (RuntimeException e) {
         // Action metadata missing or invalid - use placeholder params based on arg count
         int paramcount = NodeUtils.getActionParamCount(node);
         for (int i = 0; i < paramcount; i++) {
            try {
               AExpression exp = this.removeLastExp(false);
               params.add(exp);
            } catch (RuntimeException expEx) {
               // Stack doesn't have enough entries - use placeholder
               params.add(this.buildPlaceholderParam(i + 1));
            }
         }
         return params;
      }
      int paramcount = Math.min(NodeUtils.getActionParamCount(node), paramtypes.size());

      for (int i = 0; i < paramcount; i++) {
         Type paramtype = paramtypes.get(i);
         AExpression exp;
         try {
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
         } catch (RuntimeException expEx) {
            // Stack doesn't have enough entries - use placeholder
            exp = this.buildPlaceholderParam(i + 1);
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


