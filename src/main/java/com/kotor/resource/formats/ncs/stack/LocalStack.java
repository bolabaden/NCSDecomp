// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import java.util.LinkedList;

/**
 * Lightweight generic stack with clone support for analysis passes.
 */
public class LocalStack<T> implements Cloneable {
   protected LinkedList<T> stack = new LinkedList<>();

   public int size() {
      return this.stack.size();
   }

   @Override
   public Object clone() {
      LocalStack<T> newStack = new LocalStack<>();
      newStack.stack = new LinkedList<>(this.stack);
      return newStack;
   }

   public void close() {
      this.stack = null;
   }
}

