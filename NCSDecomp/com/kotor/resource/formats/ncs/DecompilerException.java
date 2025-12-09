// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

/**
 * Checked exception used to signal decompilation or IO failures that should be
 * presented to the user rather than crashing the UI/CLI.
 */
public class DecompilerException extends Exception {
   private static final long serialVersionUID = 1L;

   /**
    * Creates a new decompiler exception with a user-facing message.
    *
    * @param msg description of the failure
    */
   public DecompilerException(String msg) {
      super(msg);
   }
}

