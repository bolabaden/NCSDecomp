// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.StructType;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import com.kotor.resource.formats.ncs.utils.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Variable that aggregates multiple fields into a struct-like stack entry.
 */
public class VarStruct extends Variable {
   protected LinkedList<Variable> vars = new LinkedList<>();
   protected StructType structtype;

   public VarStruct() {
      super(new Type((byte)-15));
      this.size = 0;
      this.structtype = new StructType();
   }

   public VarStruct(StructType structtype) {
      this();
      this.structtype = structtype;

      List<Type> types = structtype.types();
      for (Type type : types) {
         if (StructType.class.isInstance(type)) {
            this.addVar(new VarStruct((StructType)type));
         } else {
            this.addVar(new Variable(type));
         }
      }
   }

   @Override
   public void close() {
      super.close();
      if (this.vars != null) {
         for (int i = 0; i < this.vars.size(); i++) {
            this.vars.get(i).close();
         }
      }

      this.vars = null;
      if (this.structtype != null) {
         this.structtype.close();
      }

      this.structtype = null;
   }

   public void addVar(Variable var) {
      this.vars.addFirst(var);
      var.varstruct(this);
      this.structtype.addType(var.type());
      this.size = this.size + var.size();
   }

   public void addVarStackOrder(Variable var) {
      this.vars.add(var);
      var.varstruct(this);
      this.structtype.addTypeStackOrder(var.type());
      this.size = this.size + var.size();
   }

   @Override
   public void name(String prefix, byte count) {
      this.name = prefix + "struct" + Byte.toString(count);
   }

   public String name() {
      return this.name;
   }

   public void structType(StructType structtype) {
      this.structtype = structtype;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public String typeName() {
      return this.structtype.typeName();
   }

   @Override
   public String toDeclString() {
      return this.structtype.toDeclString() + " " + this.name;
   }

   public void updateNames() {
      if (this.structtype.isVector()) {
         this.vars.get(0).name("z");
         this.vars.get(1).name("y");
         this.vars.get(2).name("x");
      } else {
         for (int i = 0; i < this.vars.size(); i++) {
            this.vars.get(i).name(this.structtype.elementName(this.vars.size() - i - 1));
         }
      }
   }

   @Override
   public void assigned() {
      for (int i = 0; i < this.vars.size(); i++) {
         this.vars.get(i).assigned();
      }
   }

   @Override
   public void addedToStack(LocalStack<?> stack) {
      for (int i = 0; i < this.vars.size(); i++) {
         this.vars.get(i).addedToStack(stack);
      }
   }

   public boolean contains(Variable var) {
      return this.vars.contains(var);
   }

   public StructType structType() {
      return this.structtype;
   }

   @Override
   public StackEntry getElement(int stackpos) {
      int pos = 0;

      for (int i = this.vars.size() - 1; i >= 0; i--) {
         StackEntry entry = this.vars.get(i);
         pos += entry.size();
         if (pos == stackpos) {
            return entry.getElement(1);
         }

         if (pos > stackpos) {
            return entry.getElement(pos - stackpos + 1);
         }
      }

      throw new RuntimeException("Stackpos was greater than stack size");
   }

   public VarStruct structify(int firstelement, int count, SubroutineAnalysisData subdata) {
      ListIterator<Variable> it = this.vars.listIterator();
      int pos = 0;

      while (it.hasNext()) {
         StackEntry entry = (StackEntry)it.next();
         pos += entry.size();
         if (pos == firstelement) {
            VarStruct varstruct = new VarStruct();
            varstruct.addVarStackOrder((Variable)entry);
            it.set(varstruct);
            entry = (StackEntry)it.next();

            for (int var8 = pos + entry.size(); var8 <= firstelement + count - 1; var8 += entry.size()) {
               it.remove();
               varstruct.addVarStackOrder((Variable)entry);
               if (!it.hasNext()) {
                  break;
               }

               entry = (StackEntry)it.next();
            }

            subdata.addStruct(varstruct);
            return varstruct;
         }

         if (pos == firstelement + count - 1) {
            return (VarStruct)entry;
         }

         if (pos > firstelement + count - 1) {
            return ((VarStruct)entry).structify(firstelement - (pos - entry.size()), count, subdata);
         }
      }

      return null;
   }
}

