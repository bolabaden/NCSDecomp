// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AAddVarCmd extends PCmd {
   private PRsaddCommand _rsaddCommand_;

   public AAddVarCmd() {
   }

   public AAddVarCmd(PRsaddCommand _rsaddCommand_) {
      this.setRsaddCommand(_rsaddCommand_);
   }

   @Override
   public Object clone() {
      return new AAddVarCmd((PRsaddCommand)this.cloneNode(this._rsaddCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAAddVarCmd(this);
   }

   public PRsaddCommand getRsaddCommand() {
      return this._rsaddCommand_;
   }

   public void setRsaddCommand(PRsaddCommand node) {
      if (this._rsaddCommand_ != null) {
         this._rsaddCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._rsaddCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._rsaddCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._rsaddCommand_ == child) {
         this._rsaddCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._rsaddCommand_ == oldChild) {
         this.setRsaddCommand((PRsaddCommand)newChild);
      }
   }
}

