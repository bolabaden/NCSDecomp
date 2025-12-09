// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptutils;

import com.kotor.resource.formats.ncs.scriptnode.ACodeBlock;
import com.kotor.resource.formats.ncs.scriptnode.AExpression;
import com.kotor.resource.formats.ncs.scriptnode.AExpressionStatement;
import com.kotor.resource.formats.ncs.scriptnode.AModifyExp;
import com.kotor.resource.formats.ncs.scriptnode.ASub;
import com.kotor.resource.formats.ncs.scriptnode.ASwitch;
import com.kotor.resource.formats.ncs.scriptnode.ASwitchCase;
import com.kotor.resource.formats.ncs.scriptnode.AVarDecl;
import com.kotor.resource.formats.ncs.scriptnode.ScriptNode;
import com.kotor.resource.formats.ncs.scriptnode.ScriptRootNode;
import com.kotor.resource.formats.ncs.stack.VarStruct;
import com.kotor.resource.formats.ncs.stack.Variable;
import com.kotor.resource.formats.ncs.utils.NodeAnalysisData;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Post-processing pass that normalizes the generated script AST before emit.
 * <p>
 * Responsibilities:
 * <ul>
 *    <li>Flatten single-block subroutines.</li>
 *    <li>Merge struct field declarations.</li>
 *    <li>Convert dangling expressions into statements.</li>
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public class CleanupPass {
   private ASub root;
   private NodeAnalysisData nodedata;
   private SubroutineAnalysisData subdata;
   private SubScriptState state;

   public CleanupPass(ASub root, NodeAnalysisData nodedata, SubroutineAnalysisData subdata, SubScriptState state) {
      this.root = root;
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.state = state;
   }

   public void apply() {
      this.checkSubCodeBlock();
      this.apply(this.root);
   }

   public void done() {
      this.root = null;
      this.nodedata = null;
      this.subdata = null;
      this.state = null;
   }

   private void checkSubCodeBlock() {
      try {
         if (this.root.size() == 1 && ACodeBlock.class.isInstance(this.root.getLastChild())) {
            ACodeBlock block = (ACodeBlock)this.root.removeLastChild();
            List<ScriptNode> children = block.removeChildren();
            this.root.addChildren(children);
         }
      } finally {
         ACodeBlock block = null;
         List<ScriptNode> children = null;
      }
   }

   private void apply(ScriptRootNode rootnode) {
      try {
         LinkedList children = rootnode.getChildren();
         ListIterator it = children.listIterator();

         while (it.hasNext()) {
            ScriptNode node1 = (ScriptNode)it.next();
            if (AVarDecl.class.isInstance(node1)) {
               Variable var = ((AVarDecl)node1).var();
               if (var != null && var.isStruct()) {
                  VarStruct struct = ((AVarDecl)node1).var().varstruct();
                  AVarDecl structdec = new AVarDecl(struct);
                  if (it.hasNext()) {
                     node1 = (ScriptNode)it.next();
                  } else {
                     node1 = null;
                  }

                  while (AVarDecl.class.isInstance(node1) && struct.equals(((AVarDecl)node1).var().varstruct())) {
                     it.remove();
                     node1.parent(null);
                     if (it.hasNext()) {
                        node1 = (ScriptNode)it.next();
                     } else {
                        node1 = null;
                     }
                  }

                  it.previous();
                  if (node1 != null) {
                     it.previous();
                  }

                  node1 = (ScriptNode)it.next();
                  structdec.parent(node1.parent());
                  it.set(structdec);
                  node1 = structdec;
               }
            }

            if (AVarDecl.class.isInstance(node1) && it.hasNext()) {
               ScriptNode node2 = (ScriptNode)it.next();
               it.previous();
               if (AExpressionStatement.class.isInstance(node2) && AModifyExp.class.isInstance(((AExpressionStatement)node2).exp())) {
                  AModifyExp modexp = (AModifyExp)((AExpressionStatement)node2).exp();
                  if (((AVarDecl)node1).var() == modexp.varRef().var()) {
                     it.remove();
                     node2.parent(null);
                     ((AVarDecl)node1).initializeExp(modexp.expression());
                  }
               }

               it.previous();
               it.next();
            }

            if (this.isDanglingExpression(node1)) {
               AExpressionStatement expstm = new AExpressionStatement((AExpression)node1);
               expstm.parent(rootnode);
               it.set(expstm);
            }

            it.previous();
            it.next();
            if (ScriptRootNode.class.isInstance(node1)) {
               this.apply((ScriptRootNode)node1);
            }

            ASwitchCase acase = null;
            if (ASwitch.class.isInstance(node1)) {
               while ((acase = ((ASwitch)node1).getNextCase(acase)) != null) {
                  this.apply(acase);
               }
            }
         }
      } finally {
         LinkedList children = null;
         ListIterator it = null;
         ScriptNode node1x = null;
         Variable var = null;
         VarStruct structx = null;
         AVarDecl structdecx = null;
         ScriptNode node2 = null;
         AModifyExp modexp = null;
         AExpressionStatement expstm = null;
         ASwitchCase acase = null;
      }
   }

   private boolean isDanglingExpression(ScriptNode node) {
      return AExpression.class.isInstance(node);
   }
}

