// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.analysis.PrunedDepthFirstAdapter;
import com.kotor.resource.formats.ncs.node.AConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopSpCommand;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AReturn;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.Node;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Marks control-flow paths that are dead/unreachable after jump analysis.
 * Uses origins from {@link SetDestinations} to trim destinations and flag
 * log-or segments appropriately.
 */
@SuppressWarnings({"unused"})
public class SetDeadCode extends PrunedDepthFirstAdapter {
   private static final byte STATE_NORMAL = 0;
   private static final byte STATE_JZ1_CP = 1;
   private static final byte STATE_JZ2_JZ = 2;
   private static final byte STATE_JZ3_CP2 = 3;
   private NodeAnalysisData nodedata;
   private SubroutineAnalysisData subdata;
   private int actionarg;
   private Hashtable<Node, ArrayList<Node>> origins;
   private Hashtable<Node, ArrayList<Node>> deadorigins;
   private byte deadstate;
   private byte state;

   public SetDeadCode(NodeAnalysisData nodedata, SubroutineAnalysisData subdata, Hashtable<Node, ArrayList<Node>> origins) {
      this.nodedata = nodedata;
      this.origins = origins;
      this.subdata = subdata;
      this.actionarg = 0;
      this.deadstate = 0;
      this.state = 0;
      this.deadorigins = new Hashtable<Node, ArrayList<Node>>(1);
   }

   public void done() {
      this.nodedata = null;
      this.subdata = null;
      this.origins = null;
      this.deadorigins = null;
   }

   @Override
   public void defaultIn(Node node) {
      if (this.actionarg > 0 && this.origins.containsKey(node)) {
         this.actionarg--;
      }

      if (this.origins.containsKey(node)) {
         this.deadstate = 0;
      } else if (this.deadorigins.containsKey(node)) {
         this.deadstate = 3;
      }

      if (NodeUtils.isCommandNode(node)) {
         this.nodedata.setCodeState(node, this.deadstate);
      }
   }

   @Override
   public void defaultOut(Node node) {
      if (NodeUtils.isCommandNode(node)) {
         this.state = 0;
      }
   }

   @Override
   public void outAConditionalJumpCommand(AConditionalJumpCommand node) {
      if (this.deadstate == 1) {
         this.removeDestination(node, this.nodedata.getDestination(node));
      } else if (this.deadstate == 3) {
         this.transferDestination(node, this.nodedata.getDestination(node));
      }

      if (NodeUtils.isJz(node)) {
         if (this.state == 1) {
            this.state++;
            return;
         }

         if (this.state == 3) {
            this.nodedata.logOrCode(node, true);
         }
      }

      this.state = 0;
   }

   @Override
   public void outACopyTopSpCommand(ACopyTopSpCommand node) {
      if (this.state != 0 && this.state != 2) {
         this.state = 0;
      } else {
         int copy = NodeUtils.stackSizeToPos(node.getSize());
         int loc = NodeUtils.stackOffsetToPos(node.getOffset());
         if (copy == 1 && loc == 1) {
            this.state++;
         } else {
            this.state = 0;
         }
      }
   }

   @Override
   public void outAJumpCommand(AJumpCommand node) {
      if (this.deadstate == 1) {
         this.removeDestination(node, this.nodedata.getDestination(node));
      } else if (this.deadstate == 3) {
         this.transferDestination(node, this.nodedata.getDestination(node));
      }

      if (this.actionarg == 0) {
         this.deadstate = 3;
      }

      this.defaultOut(node);
   }

   @Override
   public void outAStoreStateCommand(AStoreStateCommand node) {
      this.actionarg++;
      this.defaultOut(node);
   }

   public boolean isJumpToReturn(AJumpCommand node) {
      Node dest = this.nodedata.getDestination(node);
      return AReturn.class.isInstance(dest);
   }

   private void removeDestination(Node origin, Node destination) {
      this.removeDestination(origin, destination, this.origins);
   }

   private void removeDestination(Node origin, Node destination, Hashtable<Node, ArrayList<Node>> hash) {
      ArrayList<Node> originlist = hash.get(destination);
      if (originlist != null) {
         originlist.remove(origin);
         if (originlist.isEmpty()) {
            hash.remove(destination);
         }
      }
   }

   private void transferDestination(Node origin, Node destination) {
      this.removeDestination(origin, destination, this.origins);
      this.addDestination(origin, destination, this.deadorigins);
   }

   private void addDestination(Node origin, Node destination, Hashtable<Node, ArrayList<Node>> hash) {
      ArrayList<Node> originslist = hash.get(destination);
      if (originslist == null) {
         originslist = new ArrayList<>(1);
         hash.put(destination, originslist);
      }

      originslist.add(origin);
   }
}

