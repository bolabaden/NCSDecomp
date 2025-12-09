// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ADestructCmd extends PCmd {
   private PDestructCommand _destructCommand_;

   public ADestructCmd() {
   }

   public ADestructCmd(PDestructCommand _destructCommand_) {
      this.setDestructCommand(_destructCommand_);
   }

   @Override
   public Object clone() {
      return new ADestructCmd((PDestructCommand)this.cloneNode(this._destructCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseADestructCmd(this);
   }

   public PDestructCommand getDestructCommand() {
      return this._destructCommand_;
   }

   public void setDestructCommand(PDestructCommand node) {
      if (this._destructCommand_ != null) {
         this._destructCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._destructCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._destructCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._destructCommand_ == child) {
         this._destructCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._destructCommand_ == oldChild) {
         this.setDestructCommand((PDestructCommand)newChild);
      }
   }
}

