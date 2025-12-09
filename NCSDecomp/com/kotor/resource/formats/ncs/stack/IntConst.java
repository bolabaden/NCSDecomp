// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.Type;

/**
 * Constant representing an integer literal on the stack.
 */
public class IntConst extends Const {
   private Long value;

   public IntConst(Object value) {
      this.type = new Type((byte)3);
      this.value = (Long)value;
      this.size = 1;
   }

   public Long value() {
      return this.value;
   }

   @Override
   public String toString() {
      return this.value == Long.parseLong("FFFFFFFF", 16) ? "0xFFFFFFFF" : this.value.toString();
   }
}

