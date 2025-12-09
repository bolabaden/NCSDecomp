// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public class AContinueStatement extends ScriptNode {
   @Override
   public String toString() {
      return this.tabs + "continue;" + this.newline;
   }
}

