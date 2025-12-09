// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AUnaryCmd extends PCmd {
   private PUnaryCommand _unaryCommand_;

   public AUnaryCmd() {
   }

   public AUnaryCmd(PUnaryCommand _unaryCommand_) {
      this.setUnaryCommand(_unaryCommand_);
   }

   @Override
   public Object clone() {
      return new AUnaryCmd((PUnaryCommand)this.cloneNode(this._unaryCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAUnaryCmd(this);
   }

   public PUnaryCommand getUnaryCommand() {
      return this._unaryCommand_;
   }

   public void setUnaryCommand(PUnaryCommand node) {
      if (this._unaryCommand_ != null) {
         this._unaryCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._unaryCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._unaryCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._unaryCommand_ == child) {
         this._unaryCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._unaryCommand_ == oldChild) {
         this.setUnaryCommand((PUnaryCommand)newChild);
      }
   }
}

