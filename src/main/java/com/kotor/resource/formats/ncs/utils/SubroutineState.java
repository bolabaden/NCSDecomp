// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.stack.LocalTypeStack;
import com.kotor.resource.formats.ncs.stack.LocalVarStack;
import com.kotor.resource.formats.ncs.stack.VarStruct;
import com.kotor.resource.formats.ncs.stack.Variable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Tracks prototype/type information for a single subroutine during analysis.
 */
@SuppressWarnings({"unused"})
public class SubroutineState {
   private static final byte PROTO_NO = 0;
   private static final byte PROTO_IN_PROGRESS = 1;
   private static final byte PROTO_DONE = 2;
   protected static final byte JUMP_YES = 0;
   protected static final byte JUMP_NO = 1;
   protected static final byte JUMP_NA = 2;
   private Type type;
   private ArrayList<Type> params;
   private int returndepth;
   private Node root;
   private int paramsize;
   private boolean paramstyped;
   private byte status;
   private NodeAnalysisData nodedata;
   private LinkedList<DecisionData> decisionqueue;
   private byte id;

   public SubroutineState(NodeAnalysisData nodedata, Node root, byte id) {
      this.nodedata = nodedata;
      this.params = new ArrayList<>();
      this.decisionqueue = new LinkedList<>();
      this.paramstyped = true;
      this.paramsize = 0;
      this.status = 0;
      this.type = new Type((byte)0);
      this.root = root;
      this.id = id;
   }

   public void parseDone() {
      this.root = null;
      this.nodedata = null;
      this.decisionqueue = null;
   }

   public void close() {
      this.params = null;
      this.root = null;
      this.nodedata = null;
      if (this.decisionqueue != null) {
         Iterator<DecisionData> it = this.decisionqueue.iterator();

         while (it.hasNext()) {
            it.next().close();
         }

         this.decisionqueue = null;
      }

      this.type = null;
   }

   public void printState() {
      System.out.println("Return type is " + this.type);
      System.out.println("There are " + Integer.toString(this.paramsize) + " parameters");
      if (this.paramsize > 0) {
         StringBuffer buff = new StringBuffer();
         buff.append(" Types: ");

         for (Type paramType : this.params) {
            buff.append(paramType + " ");
         }

         System.out.println(buff);
      }
   }

   public void printDecisions() {
      System.out.println("-----------------------------");
      System.out.println("Jump Decisions");

      for (int i = 0; i < this.decisionqueue.size(); i++) {
         SubroutineState.DecisionData data = this.decisionqueue.get(i);
         String str = "  (" + Integer.toString(i + 1);
         str = str + ") at pos " + Integer.toString(this.nodedata.getPos(data.decisionnode));
         if (data.decision == 0) {
            str = str + " do optional jump to ";
         } else if (data.decision == 2) {
            str = str + " do required jump to ";
         } else {
            str = str + " do not jump to ";
         }

         str = str + Integer.toString(data.destination);
         System.out.println(str);
      }
   }

   public String toString(boolean main) {
      StringBuffer buff = new StringBuffer();
      buff.append(this.type + " ");
      if (main) {
         buff.append("main(");
      } else {
         buff.append("sub" + Byte.toString(this.id) + "(");
      }

      String link = "";

      for (int i = 0; i < this.paramsize; i++) {
         Type ptype = this.params.get(i);
         buff.append(link + ptype.toDeclString() + " param" + Integer.toString(i));
         link = ", ";
      }

      buff.append(")");
      return buff.toString();
   }

   public void startPrototyping() {
      this.status = 1;
   }

   public void stopPrototyping(boolean success) {
      if (success) {
         this.status = 2;
         this.decisionqueue = null;
      } else {
         this.status = 0;
      }
   }

   public boolean isPrototyped() {
      return this.status == 2;
   }

   public boolean isBeingPrototyped() {
      return this.status == 1;
   }

   public boolean isTotallyPrototyped() {
      return this.status == 2 && this.paramstyped && this.type.isTyped();
   }

   public boolean getSkipStart(int pos) {
      if (this.decisionqueue != null && !this.decisionqueue.isEmpty()) {
         SubroutineState.DecisionData decision = this.decisionqueue.getFirst();
         if (this.nodedata.getPos(decision.decisionnode) == pos) {
            if (decision.doJump()) {
               return true;
            }

            this.decisionqueue.removeFirst();
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean getSkipEnd(int pos) {
      if (this.decisionqueue != null && !this.decisionqueue.isEmpty()) {
         if (this.decisionqueue.getFirst().destination == pos) {
            this.decisionqueue.removeFirst();
            return true;
         }
      }
      return false;
   }

   public void setParamCount(int params) {
      this.paramsize = params;
      if (params > 0) {
         this.paramstyped = false;
         if (this.returndepth <= params) {
            this.type = new Type((byte)0);
         }
      }
   }

   public int getParamCount() {
      return this.paramsize;
   }

   public Type type() {
      return this.type;
   }

   public ArrayList<Type> params() {
      return this.params;
   }

   public void setReturnType(Type type, int depth) {
      this.type = type;
      this.returndepth = depth;
   }

   public void updateParams(LinkedList<Type> types) {
      new Type((byte)-1);
      this.paramstyped = true;
      boolean redo = this.params.size() > 0;
      if (types.size() < this.paramsize) {
         while (types.size() < this.paramsize) {
            types.addFirst(new Type((byte)-1));
         }
      } else if (types.size() > this.paramsize) {
         while (types.size() > this.paramsize) {
            types.removeFirst();
         }
      }

      // Ensure this.params has enough elements when redo is true
      if (redo && this.params.size() < types.size()) {
         while (this.params.size() < types.size()) {
            this.params.add(new Type((byte)-1));
         }
      }

      for (int i = 0; i < types.size(); i++) {
         Type newtype = types.get(i);

         if (redo && !this.params.get(i).isTyped()) {
            this.params.set(i, newtype);
         } else if (!redo) {
            this.params.add(newtype);
         }

         if (!this.params.get(i).isTyped()) {
            this.paramstyped = false;
         }
      }
   }

   public Type getParamType(int pos) {
      return this.params.size() < pos ? new Type((byte)0) : this.params.get(pos - 1);
   }

   public void initStack(LocalTypeStack stack) {
      if (this.isPrototyped()) {
         if (this.type.isTyped() && !this.type.equals((byte)0)) {
            if (!this.type.equals((byte)-15)) {
               stack.push(this.type);
            } else {
               ArrayList<Type> structtypes = ((StructType)this.type).types();
               for (Type structtype : structtypes) {
                  stack.push(structtype);
               }
            }
         }

         if (this.paramsize == this.params.size()) {
            for (int i = 0; i < this.paramsize; i++) {
               stack.push(this.params.get(i));
            }
         } else {
            for (int i = 0; i < this.paramsize; i++) {
               stack.push(new Type((byte)-1));
            }
         }
      }
   }

   public void initStack(LocalVarStack stack) {
      if (!this.type.equals((byte)0)) {
         Variable retvar;
         if (StructType.class.isInstance(this.type)) {
            retvar = new VarStruct((StructType)this.type);
         } else {
            retvar = new Variable(this.type);
         }

         retvar.isReturn(true);
         stack.push(retvar);
      }

      for (int i = 0; i < this.paramsize; i++) {
         Variable paramvar = new Variable(this.params.get(i));
         paramvar.isParam(true);
         stack.push(paramvar);
      }
   }

   public byte getId() {
      return this.id;
   }

   public int getStart() {
      return this.nodedata.getPos(this.root);
   }

   public int getEnd() {
      return NodeUtils.getSubEnd((ASubroutine)this.root);
   }

   public void addDecision(Node node, int destination) {
      SubroutineState.DecisionData decision = new SubroutineState.DecisionData(node, destination, false);
      this.decisionqueue.addLast(decision);
      if (this.decisionqueue.size() > 3000) {
         throw new RuntimeException("Decision queue size over 3000 - probable infinite loop");
      }
   }

   public void addJump(Node node, int destination) {
      SubroutineState.DecisionData decision = new SubroutineState.DecisionData(node, destination, true);
      this.decisionqueue.addLast(decision);
   }

   public int getCurrentDestination() {
      SubroutineState.DecisionData data = this.decisionqueue.getLast();
      if (data == null) {
         throw new RuntimeException("Attempted to get a destination but no decision nodes found.");
      }
      return data.destination;
   }

   public int switchDecision() {
      while (this.decisionqueue.size() > 0) {
         SubroutineState.DecisionData data = this.decisionqueue.getLast();
         if (data.switchDecision()) {
            return data.destination;
         }

         this.decisionqueue.removeLast();
      }

      return -1;
   }

   private class DecisionData {
      public Node decisionnode;
      public byte decision;
      public int destination;

      public DecisionData(Node node, int destination, boolean forcejump) {
         if (forcejump) {
            this.decision = 2;
         } else {
            this.decision = 1;
         }

         this.decisionnode = node;
         this.destination = destination;
      }

      public boolean doJump() {
         return this.decision != 1;
      }

      public boolean switchDecision() {
         if (this.decision == 1) {
            this.decision = 0;
            return true;
         } else {
            return false;
         }
      }

      public void close() {
         this.decisionnode = null;
      }
   }
}

