// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.stack;

import com.kotor.resource.formats.ncs.utils.Type;

/**
 * Constant representing a float literal on the stack.
 */
public class FloatConst extends Const {
   private Float value;

   public FloatConst(Float value) {
      this.type = new Type((byte)4);
      this.value = value;
      this.size = 1;
   }

   public Float value() {
      return this.value;
   }

   @Override
   public String toString() {
      // Format float to avoid scientific notation (E- or E+) which the lexer/compiler doesn't support well
      // Use DecimalFormat to ensure we get decimal notation, not scientific
      java.text.DecimalFormat df = new java.text.DecimalFormat("0.0##############");
      df.setMaximumFractionDigits(15); // Float has ~7 decimal digits of precision
      df.setMinimumFractionDigits(0);
      df.setGroupingUsed(false);
      String result = df.format(this.value);
      // Ensure we have at least one digit after the decimal point for whole-number floats
      // This is critical: 5.0 must be output as "5.0" not "5" so the compiler knows it's a float
      if (result.indexOf('.') == -1) {
         // Whole number - add .0 suffix to ensure it's treated as a float
         result = result + ".0";
      }
      return result;
   }
}

