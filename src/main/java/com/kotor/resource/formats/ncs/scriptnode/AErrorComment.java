// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.
package com.kotor.resource.formats.ncs.scriptnode;

/**
 * Lightweight placeholder node that renders a block comment.
 */
public class AErrorComment extends ScriptNode {
   private final String message;

   public AErrorComment(String message) {
      this.message = message;
   }

   @Override
   public String toString() {
      return this.tabs + "/* " + this.message + " */" + this.newline;
   }
}
