// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.lexer;

/**
 * Checked exception indicating tokenization failure in the generated lexer.
 */
public class LexerException extends Exception {
   private static final long serialVersionUID = 1L;
   public LexerException(String message) {
      super(message);
   }
}

