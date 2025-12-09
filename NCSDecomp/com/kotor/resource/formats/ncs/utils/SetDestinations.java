// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.analysis.PrunedDepthFirstAdapter;
import com.kotor.resource.formats.ncs.node.ACommandBlock;
import com.kotor.resource.formats.ncs.node.AConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.AProgram;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.node.PCmd;
import com.kotor.resource.formats.ncs.node.PSubroutine;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Maps jump instructions to their destination nodes and records origin links.
 * <p>
 * Walks the AST to resolve bytecode offsets into actual AST nodes and stores
 * them in {@link NodeAnalysisData}, also keeping an origins map for dead-code
 * analysis.
 */
@SuppressWarnings({"unused"})
public class SetDestinations extends PrunedDepthFirstAdapter {
   private NodeAnalysisData nodedata;
   private SubroutineAnalysisData subdata;
   private Node destination;
   private int currentPos;
   private Node ast;
   private int actionarg;
   private Hashtable<Node, ArrayList<Node>> origins;
   private boolean deadcode;

   public SetDestinations(Node ast, NodeAnalysisData nodedata, SubroutineAnalysisData subdata) {
      this.nodedata = nodedata;
      this.currentPos = 0;
      this.ast = ast;
      this.subdata = subdata;
      this.actionarg = 0;
      this.origins = new Hashtable<>(1);
   }

   public void done() {
      this.nodedata = null;
      this.subdata = null;
      this.destination = null;
      this.ast = null;
      this.origins = null;
   }

   public Hashtable<Node, ArrayList<Node>> getOrigins() {
      return this.origins;
   }

   @Override
   public void outAConditionalJumpCommand(AConditionalJumpCommand node) {
      int pos = NodeUtils.getJumpDestinationPos(node);
      this.lookForPos(pos, true);
      if (this.destination == null) {
         throw new RuntimeException("wasn't able to find dest for " + node + " at pos " + Integer.toString(pos));
      } else {
         this.nodedata.setDestination(node, this.destination);
         this.addDestination(node, this.destination);
      }
   }

   @Override
   public void outAJumpCommand(AJumpCommand node) {
      int pos = NodeUtils.getJumpDestinationPos(node);
      this.lookForPos(pos, true);
      if (this.destination == null) {
         throw new RuntimeException("wasn't able to find dest for " + node + " at pos " + Integer.toString(pos));
      } else {
         this.nodedata.setDestination(node, this.destination);
         if (pos < this.nodedata.getPos(node)) {
            Node dest = NodeUtils.getCommandChild(this.destination);
            this.nodedata.addOrigin(dest, node);
         }

         this.addDestination(node, this.destination);
      }
   }

   @Override
   public void outAJumpToSubroutine(AJumpToSubroutine node) {
      int pos = NodeUtils.getJumpDestinationPos(node);
      this.lookForPos(pos, false);
      if (this.destination == null) {
         throw new RuntimeException("wasn't able to find dest for " + node + " at pos " + Integer.toString(pos));
      } else {
         this.nodedata.setDestination(node, this.destination);
         this.addDestination(node, this.destination);
      }
   }

   private void addDestination(Node origin, Node destination) {
      ArrayList<Node> originslist = this.origins.get(destination);
      if (originslist == null) {
         originslist = new ArrayList<>(1);
         this.origins.put(destination, originslist);
      }

      originslist.add(origin);
   }

   private int getPos(Node node) {
      return this.nodedata.getPos(node);
   }

   private void lookForPos(final int pos, final boolean needcommand) {
      this.destination = null;
      this.ast
         .apply(
            new PrunedDepthFirstAdapter() {
               @Override
               public void defaultIn(Node node) {
                  if (SetDestinations.this.getPos(node) == pos && SetDestinations.this.destination == null && (!needcommand || NodeUtils.isCommandNode(node))) {
                     SetDestinations.this.destination = node;
                  }
               }

               @Override
               public void caseAProgram(AProgram node) {
                  this.inAProgram(node);
                  if (node.getReturn() != null) {
                     node.getReturn().apply(this);
                  }

                  Object[] temp = node.getSubroutine().toArray();
                  int cur = temp.length / 2;
                  int min = 0;
                  int max = temp.length - 1;

                  for (boolean done = SetDestinations.this.destination != null || cur >= temp.length;
                     !done;
                     done = done || SetDestinations.this.destination != null || cur > max
                  ) {
                     PSubroutine sub = (PSubroutine)temp[cur];
                     if (SetDestinations.this.getPos(sub) > pos) {
                        max = cur;
                        cur = (min + cur) / 2;
                     } else if (SetDestinations.this.getPos(sub) == pos) {
                        sub.apply(this);
                        done = true;
                     } else if (cur >= max - 1) {
                        sub.apply(this);
                        cur++;
                     } else {
                        min = cur;
                        cur = (cur + max) / 2;
                     }
                  }

                  this.outAProgram(node);
               }

               @Override
               public void caseACommandBlock(ACommandBlock node) {
                  this.inACommandBlock(node);
                  Object[] temp = node.getCmd().toArray();
                  int cur = temp.length / 2;
                  int min = 0;
                  int max = temp.length - 1;

                  for (boolean done = SetDestinations.this.destination != null || cur >= temp.length;
                     !done;
                     done = done || SetDestinations.this.destination != null || cur > max
                  ) {
                     PCmd cmd = (PCmd)temp[cur];
                     if (SetDestinations.this.getPos(cmd) > pos) {
                        max = cur;
                        cur = (min + cur) / 2;
                     } else if (SetDestinations.this.getPos(cmd) == pos) {
                        cmd.apply(this);
                        done = true;
                     } else if (cur >= max - 1) {
                        cmd.apply(this);
                        cur++;
                     } else {
                        min = cur;
                        cur = (cur + max) / 2;
                     }
                  }
               }
            }
         );
   }
}

