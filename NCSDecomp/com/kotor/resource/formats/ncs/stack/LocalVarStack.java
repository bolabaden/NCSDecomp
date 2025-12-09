// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import com.kotor.resource.formats.ncs.utils.Type;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Stack of {@link StackEntry} instances representing live variables/structs.
 * Maintains size accounting across multi-slot entries and exposes helpers to
 * group elements into structs for decompilation.
 */
public class LocalVarStack extends LocalStack<StackEntry> {
   @Override
   public void close() {
      if (this.stack != null) {
         ListIterator<StackEntry> it = this.stack.listIterator();

         while (it.hasNext()) {
            it.next().close();
         }
      }

      super.close();
   }

   public void doneParse() {
      if (this.stack != null) {
         ListIterator<StackEntry> it = this.stack.listIterator();

         while (it.hasNext()) {
            it.next().doneParse();
         }
      }

      this.stack = null;
   }

   public void doneWithStack() {
      if (this.stack != null) {
         ListIterator<StackEntry> it = this.stack.listIterator();

         while (it.hasNext()) {
            it.next().doneWithStack(this);
         }

         this.stack = null;
      }
   }

   @Override
   public int size() {
      int size = 0;
      ListIterator<StackEntry> it = this.stack.listIterator();

      while (it.hasNext()) {
         size += it.next().size();
      }

      return size;
   }

   public void push(StackEntry entry) {
      this.stack.addFirst(entry);
      entry.addedToStack(this);
   }

   public StackEntry get(int offset) {
      ListIterator<StackEntry> it = this.stack.listIterator();
      int pos = 0;

      while (it.hasNext()) {
         StackEntry entry = it.next();
         pos += entry.size();
         if (pos > offset) {
            return entry.getElement(pos - offset + 1);
         }

         if (pos == offset) {
            return entry.getElement(1);
         }
      }

      System.out.println(this.toString());
      throw new RuntimeException("offset " + Integer.valueOf(offset) + " was greater than stack size " + Integer.valueOf(pos));
   }

   public Type getType(int offset) {
      return this.get(offset).type();
   }

   public StackEntry remove() {
      StackEntry entry = this.stack.removeFirst();
      entry.removedFromStack(this);
      return entry;
   }

   public void destruct(int removesize, int savestart, int savesize, SubroutineAnalysisData subdata) {
      this.structify(1, removesize, subdata);
      if (savesize > 1) {
         this.structify(removesize - (savestart + savesize) + 1, savesize, subdata);
      }

      Variable struct = (Variable)this.stack.getFirst();
      Variable element = (Variable)struct.getElement(removesize - (savestart + savesize) + 1);
      this.stack.set(0, element);
   }

   public VarStruct structify(int firstelement, int count, SubroutineAnalysisData subdata) {
      ListIterator<StackEntry> it = this.stack.listIterator();
      int pos = 0;

      while (it.hasNext()) {
         StackEntry entry = it.next();
         pos += entry.size();
         if (pos == firstelement) {
            VarStruct varstruct = new VarStruct();
            varstruct.addVarStackOrder((Variable)entry);
            it.set(varstruct);
            entry = it.next();

            for (int var8 = pos + entry.size(); var8 <= firstelement + count - 1; var8 += entry.size()) {
               it.remove();
               varstruct.addVarStackOrder((Variable)entry);
               if (!it.hasNext()) {
                  break;
               }

               entry = it.next();
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

   @Override
   public String toString() {
      String newline = System.getProperty("line.separator");
      StringBuffer buffer = new StringBuffer();
      int max = this.stack.size();
      buffer.append("---stack, size " + Integer.toString(max) + "---" + newline);

      for (int i = 0; i < max; i++) {
         StackEntry entry = this.stack.get(i);
         buffer.append("-->" + Integer.toString(i) + entry.toString() + newline);
      }

      return buffer.toString();
   }

   @Override
   public Object clone() {
      LocalVarStack newStack = new LocalVarStack();
      newStack.stack = new LinkedList<>(this.stack);

      for (StackEntry entry : this.stack) {
         if (Variable.class.isInstance(entry)) {
            ((Variable)entry).stackWasCloned(this, newStack);
         }
      }

      return newStack;
   }
}

