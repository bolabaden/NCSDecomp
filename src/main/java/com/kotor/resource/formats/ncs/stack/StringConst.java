// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.Type;

/**
 * Constant representing a string literal on the stack.
 */
public class StringConst extends Const {
   private String value;

   public StringConst(Object value) {
      this.type = new Type((byte)5);
      this.value = (String)value;
      this.size = 1;
   }

   public String value() {
      return this.value;
   }

   @Override
   public String toString() {
      return this.value.toString();
   }
}

