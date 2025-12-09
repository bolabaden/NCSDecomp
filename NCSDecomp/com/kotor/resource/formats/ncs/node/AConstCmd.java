// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AConstCmd extends PCmd {
   private PConstCommand _constCommand_;

   public AConstCmd() {
   }

   public AConstCmd(PConstCommand _constCommand_) {
      this.setConstCommand(_constCommand_);
   }

   @Override
   public Object clone() {
      return new AConstCmd((PConstCommand)this.cloneNode(this._constCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAConstCmd(this);
   }

   public PConstCommand getConstCommand() {
      return this._constCommand_;
   }

   public void setConstCommand(PConstCommand node) {
      if (this._constCommand_ != null) {
         this._constCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._constCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._constCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._constCommand_ == child) {
         this._constCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._constCommand_ == oldChild) {
         this.setConstCommand((PConstCommand)newChild);
      }
   }
}

