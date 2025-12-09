// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

public abstract class ScriptNode {
   private ScriptNode parent;
   protected String tabs;
   protected String newline = System.getProperty("line.separator");

   public ScriptNode parent() {
      return this.parent;
   }

   public void parent(ScriptNode parent) {
      this.parent = parent;
      if (parent != null) {
         this.tabs = parent.tabs + "\t";
      }
   }

   public void close() {
      this.parent = null;
   }
}

