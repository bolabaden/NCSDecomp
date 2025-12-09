// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AMovespCmd extends PCmd {
   private PMoveSpCommand _moveSpCommand_;

   public AMovespCmd() {
   }

   public AMovespCmd(PMoveSpCommand _moveSpCommand_) {
      this.setMoveSpCommand(_moveSpCommand_);
   }

   @Override
   public Object clone() {
      return new AMovespCmd((PMoveSpCommand)this.cloneNode(this._moveSpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAMovespCmd(this);
   }

   public PMoveSpCommand getMoveSpCommand() {
      return this._moveSpCommand_;
   }

   public void setMoveSpCommand(PMoveSpCommand node) {
      if (this._moveSpCommand_ != null) {
         this._moveSpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._moveSpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._moveSpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._moveSpCommand_ == child) {
         this._moveSpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._moveSpCommand_ == oldChild) {
         this.setMoveSpCommand((PMoveSpCommand)newChild);
      }
   }
}

