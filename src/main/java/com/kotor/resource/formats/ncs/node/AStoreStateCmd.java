// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AStoreStateCmd extends PCmd {
   private PStoreStateCommand _storeStateCommand_;

   public AStoreStateCmd() {
   }

   public AStoreStateCmd(PStoreStateCommand _storeStateCommand_) {
      this.setStoreStateCommand(_storeStateCommand_);
   }

   @Override
   public Object clone() {
      return new AStoreStateCmd((PStoreStateCommand)this.cloneNode(this._storeStateCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAStoreStateCmd(this);
   }

   public PStoreStateCommand getStoreStateCommand() {
      return this._storeStateCommand_;
   }

   public void setStoreStateCommand(PStoreStateCommand node) {
      if (this._storeStateCommand_ != null) {
         this._storeStateCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._storeStateCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._storeStateCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._storeStateCommand_ == child) {
         this._storeStateCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._storeStateCommand_ == oldChild) {
         this.setStoreStateCommand((PStoreStateCommand)newChild);
      }
   }
}

