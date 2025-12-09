// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AActionCmd extends PCmd {
   private PActionCommand _actionCommand_;

   public AActionCmd() {
   }

   public AActionCmd(PActionCommand _actionCommand_) {
      this.setActionCommand(_actionCommand_);
   }

   @Override
   public Object clone() {
      return new AActionCmd((PActionCommand)this.cloneNode(this._actionCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAActionCmd(this);
   }

   public PActionCommand getActionCommand() {
      return this._actionCommand_;
   }

   public void setActionCommand(PActionCommand node) {
      if (this._actionCommand_ != null) {
         this._actionCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._actionCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._actionCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._actionCommand_ == child) {
         this._actionCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._actionCommand_ == oldChild) {
         this.setActionCommand((PActionCommand)newChild);
      }
   }
}

