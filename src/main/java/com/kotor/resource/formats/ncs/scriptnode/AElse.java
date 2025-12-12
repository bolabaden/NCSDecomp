// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public class AElse extends ScriptRootNode {
   public AElse(int start, int end) {
      super(start, end);
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();
      
      // Handle "else if" case: if the first (and only) child is an AIf, output "else if" instead of "else { if ... }"
      if (this.children.size() == 1 && AIf.class.isInstance(this.children.get(0))) {
         AIf ifChild = (AIf) this.children.get(0);
         // Format condition similar to AControlLoop.formattedCondition()
         String cond;
         if (ifChild.condition() == null) {
            cond = " ()";
         } else {
            String condStr = ifChild.condition().toString().trim();
            boolean wrapped = condStr.startsWith("(") && condStr.endsWith(")");
            cond = wrapped ? condStr : "(" + condStr + ")";
            cond = " " + cond;
         }
         buff.append(this.tabs + "else if" + cond + " {" + this.newline);
         
         for (int i = 0; i < ifChild.children.size(); i++) {
            buff.append(ifChild.children.get(i).toString());
         }
         
         buff.append(this.tabs + "}" + this.newline);
      } else {
         // Standard else block
         buff.append(this.tabs + "else {" + this.newline);

         for (int i = 0; i < this.children.size(); i++) {
            buff.append(this.children.get(i).toString());
         }

         buff.append(this.tabs + "}" + this.newline);
      }
      
      return buff.toString();
   }
}

