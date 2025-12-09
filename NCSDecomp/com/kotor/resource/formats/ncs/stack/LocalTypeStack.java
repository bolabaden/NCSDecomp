// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.SubroutineState;
import com.kotor.resource.formats.ncs.utils.Type;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Stack that tracks only {@link Type} metadata during prototyping passes.
 */
public class LocalTypeStack extends LocalStack<Type> {
   public void push(Type type) {
      this.stack.addFirst(type);
   }

   public Type get(int offset) {
      ListIterator<Type> it = this.stack.listIterator();
      int pos = 0;

      while (it.hasNext()) {
         Type type = it.next();
         pos += type.size();
         if (pos > offset) {
            return type.getElement(pos - offset + 1);
         }

         if (pos == offset) {
            return type.getElement(1);
         }
      }

      return new Type((byte)-1);
   }

   public Type get(int offset, SubroutineState state) {
      ListIterator<Type> it = this.stack.listIterator();
      int pos = 0;

      while (it.hasNext()) {
         Type type = it.next();
         pos += type.size();
         if (pos > offset) {
            return type.getElement(pos - offset + 1);
         }

         if (pos == offset) {
            return type.getElement(1);
         }
      }

      if (state.isPrototyped()) {
         Type typex = state.getParamType(offset - pos);
         if (!typex.equals((byte)0)) {
            return typex;
         }
      }

      return new Type((byte)-1);
   }

   public void remove(int count) {
      for (int i = 0; i < count; i++) {
         this.stack.removeFirst();
      }
   }

   public void removeParams(int count, SubroutineState state) {
      LinkedList<Type> params = new LinkedList<>();

      for (int i = 0; i < count; i++) {
         Type type = this.stack.removeFirst();
         params.addFirst(type);
      }

      state.updateParams(params);
   }

   public int removePrototyping(int count) {
      int params = 0;
      int i = 0;

      while (i < count) {
         if (this.stack.isEmpty()) {
            params++;
            i++;
         } else {
            Type type = this.stack.removeFirst();
            i += type.size();
         }
      }

      return params;
   }

   public void remove(int start, int count) {
      int loc = start - 1;

      for (int i = 0; i < count; i++) {
         this.stack.remove(loc);
      }
   }

   @Override
   public String toString() {
      String newline = System.getProperty("line.separator");
      StringBuffer buffer = new StringBuffer();
      int max = this.stack.size();
      buffer.append("---stack, size " + Integer.toString(max) + "---" + newline);

      for (int i = 1; i <= max; i++) {
         Type type = this.stack.get(max - i);
         buffer.append("-->" + Integer.toString(i) + " is type " + type + newline);
      }

      return buffer.toString();
   }

   @Override
   public Object clone() {
      LocalTypeStack newStack = new LocalTypeStack();
      newStack.stack = new LinkedList<>(this.stack);
      return newStack;
   }
}

