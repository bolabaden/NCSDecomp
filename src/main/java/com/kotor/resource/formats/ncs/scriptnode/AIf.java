// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public class AIf extends AControlLoop {
   public AIf(int start, int end, AExpression condition) {
      super(start, end);
      this.condition(condition);
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();
      buff.append(this.tabs + "if (" + this.condition.toString() + ") {" + this.newline);

      for (int i = 0; i < this.children.size(); i++) {
         buff.append(this.children.get(i).toString());
      }

      buff.append(this.tabs + "}" + this.newline);
      return buff.toString();
   }
}

