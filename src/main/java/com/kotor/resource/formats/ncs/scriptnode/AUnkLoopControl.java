// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public class AUnkLoopControl extends ScriptNode {
   protected int dest;

   public AUnkLoopControl(int dest) {
      this.dest = dest;
   }

   public int getDestination() {
      return this.dest;
   }

   @Override
   public String toString() {
      return "BREAK or CONTINUE undetermined";
   }
}

