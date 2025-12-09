// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AJumpCmd extends PCmd {
   private PJumpCommand _jumpCommand_;

   public AJumpCmd() {
   }

   public AJumpCmd(PJumpCommand _jumpCommand_) {
      this.setJumpCommand(_jumpCommand_);
   }

   @Override
   public Object clone() {
      return new AJumpCmd((PJumpCommand)this.cloneNode(this._jumpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAJumpCmd(this);
   }

   public PJumpCommand getJumpCommand() {
      return this._jumpCommand_;
   }

   public void setJumpCommand(PJumpCommand node) {
      if (this._jumpCommand_ != null) {
         this._jumpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._jumpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._jumpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._jumpCommand_ == child) {
         this._jumpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._jumpCommand_ == oldChild) {
         this.setJumpCommand((PJumpCommand)newChild);
      }
   }
}

