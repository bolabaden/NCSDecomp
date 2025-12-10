// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.node.AProgram;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.node.PSubroutine;
import com.kotor.resource.formats.ncs.node.Start;
import com.kotor.resource.formats.ncs.scriptutils.SubScriptState;
import com.kotor.resource.formats.ncs.stack.LocalVarStack;
import com.kotor.resource.formats.ncs.stack.VarStruct;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

public class SubroutineAnalysisData {
   private NodeAnalysisData nodedata;
   private Hashtable<Integer, ASubroutine> subroutines;
   private Hashtable<Node, SubroutineState> substates;
   private ASubroutine mainsub;
   private ASubroutine globalsub;
   private LocalVarStack globalstack;
   private ArrayList<StructType> globalstructs;
   private SubScriptState globalstate;

   public SubroutineAnalysisData(NodeAnalysisData nodedata) {
      this.nodedata = nodedata;
      this.subroutines = new Hashtable<>(1);
      this.substates = new Hashtable<>(1);
      this.globalsub = null;
      this.globalstack = null;
      this.mainsub = null;
      this.globalstructs = new ArrayList<>();
   }

   public void parseDone() {
      this.nodedata = null;
      if (this.substates != null) {
         Enumeration<SubroutineState> subs = this.substates.elements();

         while (subs.hasMoreElements()) {
            subs.nextElement().parseDone();
         }

         subs = null;
         this.substates = null;
      }

      this.subroutines = null;
      this.mainsub = null;
      this.globalsub = null;
      this.globalstate = null;
   }

   public void close() {
      if (this.nodedata != null) {
         this.nodedata.close();
         this.nodedata = null;
      }

      if (this.substates != null) {
         Enumeration<SubroutineState> subs = this.substates.elements();

         while (subs.hasMoreElements()) {
            subs.nextElement().close();
         }

         this.substates = null;
         subs = null;
      }

      if (this.subroutines != null) {
         this.subroutines.clear();
         this.subroutines = null;
      }

      this.mainsub = null;
      this.globalsub = null;
      if (this.globalstack != null) {
         this.globalstack.close();
         this.globalstack = null;
      }

      if (this.globalstructs != null) {
         Iterator<StructType> it = this.globalstructs.iterator();

         while (it.hasNext()) {
            it.next().close();
         }

         this.globalstructs = null;
      }

      if (this.globalstate != null) {
         this.globalstate.close();
         this.globalstate = null;
      }
   }

   public void printStates() {
      Enumeration<Node> subnodes = this.substates.keys();

      while (subnodes.hasMoreElements()) {
         Node node = subnodes.nextElement();
         SubroutineState state = this.substates.get(node);
         System.out.println("Printing state for subroutine at " + Integer.toString(this.nodedata.getPos(node)));
         state.printState();
      }
   }

   public SubScriptState globalState() {
      return this.globalstate;
   }

   public void globalState(SubScriptState globalstate) {
      this.globalstate = globalstate;
   }

   public ASubroutine getGlobalsSub() {
      return this.globalsub;
   }

   public void setGlobalsSub(ASubroutine globalsub) {
      this.globalsub = globalsub;
   }

   public ASubroutine getMainSub() {
      return this.mainsub;
   }

   public void setMainSub(ASubroutine mainsub) {
      this.mainsub = mainsub;
   }

   public LocalVarStack getGlobalStack() {
      return this.globalstack;
   }

   public void setGlobalStack(LocalVarStack stack) {
      this.globalstack = stack;
   }

   public int numSubs() {
      return this.subroutines.size();
   }

   public int countSubsDone() {
      Enumeration<SubroutineState> subs = this.substates.elements();
      int count = 0;

      while (subs.hasMoreElements()) {
         if (subs.nextElement().isTotallyPrototyped()) {
            count++;
         }
      }

      return count;
   }

   public SubroutineState getState(Node sub) {
      return this.substates.get(sub);
   }

   public boolean isPrototyped(int pos, boolean nullok) {
      ASubroutine sub = this.subroutines.get(pos);
      if (sub == null) {
         if (nullok) {
            return false;
         } else {
            throw new RuntimeException("Checking prototype on a subroutine not in the hash");
         }
      } else {
         SubroutineState state = this.substates.get(sub);
         return state != null && state.isPrototyped();
      }
   }

   public boolean isBeingPrototyped(int pos) {
      ASubroutine sub = this.subroutines.get(pos);
      if (sub == null) {
         throw new RuntimeException("Checking prototype on a subroutine not in the hash");
      } else {
         SubroutineState state = this.substates.get(sub);
         return state != null && state.isBeingPrototyped();
      }
   }

   public boolean isFullyPrototyped(int pos) {
      ASubroutine sub = this.subroutines.get(pos);
      if (sub == null) {
         throw new RuntimeException("Checking prototype on a subroutine not in the hash");
      } else {
         SubroutineState state = this.substates.get(sub);
         return state != null && state.isTotallyPrototyped();
      }
   }

   public void addStruct(StructType struct) {
      if (!this.globalstructs.contains(struct)) {
         this.globalstructs.add(struct);
         struct.typeName("structtype" + this.globalstructs.size());
      }
   }

   public void addStruct(VarStruct struct) {
      StructType structtype = struct.structType();
      if (!this.globalstructs.contains(structtype)) {
         this.globalstructs.add(structtype);
         structtype.typeName("structtype" + this.globalstructs.size());
      } else {
         struct.structType(this.getStructPrototype(structtype));
      }
   }

   public String getStructDeclarations() {
      String newline = System.getProperty("line.separator");
      StringBuffer buff = new StringBuffer();

      for (int i = 0; i < this.globalstructs.size(); i++) {
         StructType structtype = this.globalstructs.get(i);
         if (!structtype.isVector()) {
            buff.append(structtype.toDeclString() + " {" + newline);
            ArrayList<Type> types = structtype.types();

            for (int j = 0; j < types.size(); j++) {
               buff.append("\t" + types.get(j).toDeclString() + " " + structtype.elementName(j) + ";" + newline);
            }

            buff.append("};" + newline + newline);
            types = null;
         }
      }

      return buff.toString();
   }

   public String getStructTypeName(StructType structtype) {
      StructType protostruct = this.getStructPrototype(structtype);
      return protostruct.typeName();
   }

   public StructType getStructPrototype(StructType structtype) {
      int index = this.globalstructs.indexOf(structtype);
      if (index == -1) {
         this.globalstructs.add(structtype);
         index = this.globalstructs.size() - 1;
      }

      return this.globalstructs.get(index);
   }

   private void addSubroutine(int pos, ASubroutine node, byte id) {
      this.subroutines.put(pos, node);
      this.addSubState(node, id);
   }

   private void addSubState(Node sub, byte id) {
      SubroutineState state = new SubroutineState(this.nodedata, sub, id);
      this.substates.put(sub, state);
   }

   private void addSubState(Node sub, byte id, Type type) {
      SubroutineState state = new SubroutineState(this.nodedata, sub, id);
      state.setReturnType(type, 1);
      this.substates.put(sub, state);
   }

   private void addMain(ASubroutine sub, boolean conditional) {
      this.mainsub = sub;
      if (conditional) {
         this.addSubState(this.mainsub, (byte)0, new Type((byte)3));
      } else {
         this.addSubState(this.mainsub, (byte)0);
      }
   }

   private void addGlobals(ASubroutine sub) {
      this.globalsub = sub;
   }

   public Iterator<ASubroutine> getSubroutines() {
      ArrayList<ASubroutine> subs = new ArrayList<>();
      TreeSet<Integer> keys = new TreeSet<>();
      keys.addAll(this.subroutines.keySet());
      Iterator<Integer> it = keys.iterator();

      while (it.hasNext()) {
         subs.add(this.subroutines.get(it.next()));
      }

      return subs.iterator();
   }

   public void splitOffSubroutines(Start ast) {
      boolean conditional = NodeUtils.isConditionalProgram(ast);
      LinkedList<PSubroutine> subroutines = ((AProgram)ast.getPProgram()).getSubroutine();
      ASubroutine node = (ASubroutine)subroutines.remove(0);
      if (subroutines.size() > 0 && this.isGlobalsSub(node)) {
         this.addGlobals(node);
         node = (ASubroutine)subroutines.remove(0);
      }

      this.addMain(node, conditional);
      byte id = 1;

      while (subroutines.size() > 0) {
         node = (ASubroutine)subroutines.remove(0);
         this.addSubroutine(this.nodedata.getPos(node), node, id++);
      }
   }

   private boolean isGlobalsSub(ASubroutine node) {
      CheckIsGlobals cig = new CheckIsGlobals();
      node.apply(cig);
      return cig.getIsGlobals();
   }
}

