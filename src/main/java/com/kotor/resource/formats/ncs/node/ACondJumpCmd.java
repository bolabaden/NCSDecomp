// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ACondJumpCmd extends PCmd {
   private PConditionalJumpCommand _conditionalJumpCommand_;

   public ACondJumpCmd() {
   }

   public ACondJumpCmd(PConditionalJumpCommand _conditionalJumpCommand_) {
      this.setConditionalJumpCommand(_conditionalJumpCommand_);
   }

   @Override
   public Object clone() {
      return new ACondJumpCmd((PConditionalJumpCommand)this.cloneNode(this._conditionalJumpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACondJumpCmd(this);
   }

   public PConditionalJumpCommand getConditionalJumpCommand() {
      return this._conditionalJumpCommand_;
   }

   public void setConditionalJumpCommand(PConditionalJumpCommand node) {
      if (this._conditionalJumpCommand_ != null) {
         this._conditionalJumpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._conditionalJumpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._conditionalJumpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._conditionalJumpCommand_ == child) {
         this._conditionalJumpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._conditionalJumpCommand_ == oldChild) {
         this.setConditionalJumpCommand((PConditionalJumpCommand)newChild);
      }
   }
}

