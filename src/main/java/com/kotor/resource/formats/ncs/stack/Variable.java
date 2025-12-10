// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.Type;
import java.util.Hashtable;

/**
 * Represents a variable or struct element on the logical variable stack.
 */
public class Variable extends StackEntry implements Comparable<Variable> {
   protected static final byte FCN_NORMAL = 0;
   protected static final byte FCN_RETURN = 1;
   protected static final byte FCN_PARAM = 2;
   private Hashtable<LocalStack<?>, Integer> stackcounts;
   protected String name;
   protected boolean assigned;
   protected VarStruct varstruct;
   protected byte function;

   public Variable(Type type) {
      this.type = type;
      this.varstruct = null;
      this.assigned = false;
      this.size = 1;
      this.function = 0;
      this.stackcounts = new Hashtable<>(1);
   }

   public Variable(byte type) {
      this(new Type(type));
   }

   @Override
   public void close() {
      super.close();
      this.stackcounts = null;
      this.varstruct = null;
   }

   @Override
   public void doneParse() {
      this.stackcounts = null;
   }

   @Override
   public void doneWithStack(LocalVarStack stack) {
      this.stackcounts.remove(stack);
   }

   public void isReturn(boolean isreturn) {
      if (isreturn) {
         this.function = 1;
      } else {
         this.function = 0;
      }
   }

   public void isParam(boolean isparam) {
      if (isparam) {
         this.function = 2;
      } else {
         this.function = 0;
      }
   }

   public boolean isReturn() {
      return this.function == 1;
   }

   public boolean isParam() {
      return this.function == 2;
   }

   public void assigned() {
      this.assigned = true;
   }

   public boolean isAssigned() {
      return this.assigned;
   }

   public boolean isStruct() {
      return this.varstruct != null;
   }

   public void varstruct(VarStruct varstruct) {
      this.varstruct = varstruct;
   }

   public VarStruct varstruct() {
      return this.varstruct;
   }

   @Override
   public void addedToStack(LocalStack<?> stack) {
      Integer count = this.stackcounts.get(stack);
      if (count == null) {
         this.stackcounts.put(stack, Integer.valueOf(1));
      } else {
         this.stackcounts.put(stack, Integer.valueOf(count + 1));
      }
   }

   @Override
   public void removedFromStack(LocalStack<?> stack) {
      Integer count = this.stackcounts.get(stack);
      if (count != null && count != 0) {
         this.stackcounts.put(stack, Integer.valueOf(count - 1));
      } else {
         this.stackcounts.remove(stack);
      }
   }

   public boolean isPlaceholder(LocalStack<?> stack) {
      Integer count = this.stackcounts.get(stack);
      return count == null ? true : count == 0 && !this.assigned;
   }

   public boolean isOnStack(LocalStack<?> stack) {
      Integer count = this.stackcounts.get(stack);
      return count == null ? false : count > 0;
   }

   public void name(String prefix, byte hint) {
      this.name = prefix + this.type.toString() + Byte.toString(hint);
   }

   public void name(String infix, int hint) {
      this.name = this.type.toString() + infix + Integer.toString(hint);
   }

   public void name(String name) {
      this.name = name;
   }

   @Override
   public StackEntry getElement(int stackpos) {
      if (stackpos != 1) {
         throw new RuntimeException("Position > 1 for var, not struct");
      } else {
         return this;
      }
   }

   public String toDebugString() {
      return "type: " + this.type + " name: " + this.name + " assigned: " + Boolean.toString(this.assigned);
   }

   @Override
   public String toString() {
      if (this.varstruct != null) {
         this.varstruct.updateNames();
         return this.varstruct.name() + "." + this.name;
      } else {
         return this.name;
      }
   }

   public String toDeclString() {
      return this.type + " " + this.name;
   }

   @Override
   public int compareTo(Variable o) throws ClassCastException {
      if (o == null) {
         throw new NullPointerException();
      } else if (this == o) {
         return 0;
      } else if (this.name == null) {
         return -1;
      } else {
         return o.name == null ? 1 : this.name.compareTo(o.name);
      }
   }

   public void stackWasCloned(LocalStack<?> oldstack, LocalStack<?> newstack) {
      Integer count = this.stackcounts.get(oldstack);
      if (count != null && count > 0) {
         this.stackcounts.put(newstack, count);
      }
   }
}

