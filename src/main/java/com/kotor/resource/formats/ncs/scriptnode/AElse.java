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
      buff.append(this.tabs + "else {" + this.newline);

      for (int i = 0; i < this.children.size(); i++) {
         buff.append(this.children.get(i).toString());
      }

      buff.append(this.tabs + "}" + this.newline);
      return buff.toString();
   }
}

