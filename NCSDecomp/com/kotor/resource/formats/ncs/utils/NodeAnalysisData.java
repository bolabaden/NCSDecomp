// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.stack.LocalStack;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Per-node metadata container used by analysis passes (positions, jump targets,
 * stack snapshots, dead-code markers, log-or markers, origins).
 */
public class NodeAnalysisData {
   private Hashtable<Node, NodeData> nodedatahash = new Hashtable<>(1);

   public void close() {
      if (this.nodedatahash != null) {
         Enumeration<NodeData> data = this.nodedatahash.elements();

         while (data.hasMoreElements()) {
            data.nextElement().close();
         }

         this.nodedatahash = null;
      }
   }

   public void setPos(Node node, int pos) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         data = new NodeAnalysisData.NodeData(pos);
         this.nodedatahash.put(node, data);
      } else {
         data.pos = pos;
      }
   }

   public int getPos(Node node) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to read position on a node not in the hashtable.");
      } else {
         return data.pos;
      }
   }

   public void setDestination(Node jump, Node destination) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(jump);
      if (data == null) {
         data = new NodeAnalysisData.NodeData();
         data.jumpDestination = destination;
         this.nodedatahash.put(jump, data);
      } else {
         data.jumpDestination = destination;
      }
   }

   public Node getDestination(Node node) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to read destination on a node not in the hashtable.");
      } else {
         return data.jumpDestination;
      }
   }

   public void setCodeState(Node node, byte state) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         data = new NodeAnalysisData.NodeData();
         data.state = state;
         this.nodedatahash.put(node, data);
      } else {
         data.state = state;
      }
   }

   public void deadCode(Node node, boolean deadcode) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to set status on a node not in the hashtable.");
      } else {
         if (deadcode) {
            data.state = 1;
         } else {
            data.state = 0;
         }
      }
   }

   public boolean deadCode(Node node) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to read status on a node not in the hashtable.");
      } else {
         return data.state == 1 || data.state == 3;
      }
   }

   public boolean processCode(Node node) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to read status on a node not in the hashtable.");
      } else {
         return data.state != 1;
      }
   }

   public void logOrCode(Node node, boolean logor) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to set status on a node not in the hashtable.");
      } else {
         if (logor) {
            data.state = 2;
         } else {
            data.state = 0;
         }
      }
   }

   public boolean logOrCode(Node node) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to read status on a node not in the hashtable.");
      } else {
         return data.state == 2;
      }
   }

   public void addOrigin(Node node, Node origin) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         data = new NodeAnalysisData.NodeData();
         data.addOrigin(origin);
         this.nodedatahash.put(node, data);
      } else {
         data.addOrigin(origin);
      }
   }

   public Node removeLastOrigin(Node node) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         throw new RuntimeException("Attempted to read origin on a node not in the hashtable.");
      } else {
         return data.origins != null && !data.origins.isEmpty() ? data.origins.remove(data.origins.size() - 1) : null;
      }
   }

   public void setStack(Node node, LocalStack<?> stack, boolean overwrite) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      if (data == null) {
         data = new NodeAnalysisData.NodeData();
         data.stack = stack;
         this.nodedatahash.put(node, data);
      } else if (data.stack == null || overwrite) {
         data.stack = stack;
      }
   }

   public LocalStack<?> getStack(Node node) {
      NodeAnalysisData.NodeData data = this.nodedatahash.get(node);
      return data.stack;
   }

   public void clearProtoData() {
      Enumeration<NodeData> e = this.nodedatahash.elements();

      while (e.hasMoreElements()) {
         e.nextElement().stack = null;
      }
   }

   public class NodeData {
      public static final byte STATE_NORMAL = 0;
      public static final byte STATE_DEAD = 1;
      public static final byte STATE_LOGOR = 2;
      public static final byte STATE_DEAD_PROCESS = 3;
      public int pos;
      public Node jumpDestination;
      public LocalStack<?> stack;
      public byte state;
      public ArrayList<Node> origins;

      public NodeData() {
         this.pos = -1;
         this.jumpDestination = null;
         this.stack = null;
         this.state = 0;
      }

      public NodeData(int pos) {
         this.jumpDestination = null;
         this.pos = pos;
         this.stack = null;
         this.state = 0;
      }

      public void addOrigin(Node origin) {
         if (this.origins == null) {
            this.origins = new ArrayList<>();
         }

         this.origins.add(origin);
      }

      public void close() {
         this.jumpDestination = null;
         this.stack = null;
         this.origins = null;
      }
   }
}

