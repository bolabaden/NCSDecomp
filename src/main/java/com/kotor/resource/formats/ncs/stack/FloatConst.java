// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.Type;

/**
 * Constant representing a float literal on the stack.
 */
public class FloatConst extends Const {
   private Float value;

   public FloatConst(Object value) {
      this.type = new Type((byte)4);
      this.value = (Float)value;
      this.size = 1;
   }

   public Float value() {
      return this.value;
   }

   @Override
   public String toString() {
      return this.value.toString();
   }
}

