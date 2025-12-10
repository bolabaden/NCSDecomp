// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.Type;

/**
 * Base class for constant stack entries (int/float/string/object).
 */
public class Const extends StackEntry {
   public static Const newConst(Type type, Object value) {
      switch (type.byteValue()) {
         case 3:
            return new IntConst(value);
         case 4:
            return new FloatConst(value);
         case 5:
            return new StringConst(value);
         case 6:
            return new ObjectConst(value);
         default:
            throw new RuntimeException("Invalid const type " + type);
      }
   }

   @Override
   public void removedFromStack(LocalStack<?> stack) {
   }

   @Override
   public void addedToStack(LocalStack<?> stack) {
   }

   @Override
   public void doneParse() {
   }

   @Override
   public void doneWithStack(LocalVarStack stack) {
   }

   @Override
   public String toString() {
      return "";
   }

   @Override
   public StackEntry getElement(int stackpos) {
      if (stackpos != 1) {
         throw new RuntimeException("Position > 1 for const, not struct");
      } else {
         return this;
      }
   }
}

