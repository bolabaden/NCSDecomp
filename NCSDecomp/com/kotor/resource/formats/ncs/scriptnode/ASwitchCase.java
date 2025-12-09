// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import java.util.ArrayList;
import java.util.List;

public class ASwitchCase extends ScriptRootNode {
   protected AConst val;

   public ASwitchCase(int start, AConst val) {
      super(start, -1);
      this.val(val);
   }

   public ASwitchCase(int start) {
      super(start, -1);
   }

   public void end(int end) {
      this.end = end;
   }

   private void val(AConst val) {
      val.parent(this);
      this.val = val;
   }

   public List<AUnkLoopControl> getUnknowns() {
      List<AUnkLoopControl> unks = new ArrayList<>();

      for (ScriptNode node : this.children) {
         if (AUnkLoopControl.class.isInstance(node)) {
            unks.add((AUnkLoopControl)node);
         }
      }

      return unks;
   }

   public void replaceUnknown(AUnkLoopControl unk, ScriptNode newnode) {
      newnode.parent(this);
      this.children.set(this.children.indexOf(unk), newnode);
      unk.parent(null);
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();
      if (this.val == null) {
         buff.append(this.tabs + "default:" + this.newline);
      } else {
         buff.append(this.tabs + "case " + this.val.toString() + ":" + this.newline);
      }

      for (int i = 0; i < this.children.size(); i++) {
         buff.append(this.children.get(i).toString());
      }

      return buff.toString();
   }

   @Override
   public void close() {
      super.close();
      if (this.val != null) {
         this.val.close();
      }

      this.val = null;
   }
}

