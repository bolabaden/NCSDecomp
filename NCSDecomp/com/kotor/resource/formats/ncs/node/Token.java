// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

/**
 * Base class for all lexer tokens; stores text and source position.
 */
public abstract class Token extends Node {
   private String text;
   private int line;
   private int pos;

   public String getText() {
      return this.text;
   }

   public void setText(String text) {
      this.text = text;
   }

   public int getLine() {
      return this.line;
   }

   public void setLine(int line) {
      this.line = line;
   }

   public int getPos() {
      return this.pos;
   }

   public void setPos(int pos) {
      this.pos = pos;
   }

   @Override
   public String toString() {
      return this.text + " ";
   }

   @Override
   void removeChild(Node child) {
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
   }
}

