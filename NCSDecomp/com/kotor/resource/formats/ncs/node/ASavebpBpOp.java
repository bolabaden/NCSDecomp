// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ASavebpBpOp extends PBpOp {
   private TSavebp _savebp_;

   public ASavebpBpOp() {
   }

   public ASavebpBpOp(TSavebp _savebp_) {
      this.setSavebp(_savebp_);
   }

   @Override
   public Object clone() {
      return new ASavebpBpOp((TSavebp)this.cloneNode(this._savebp_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseASavebpBpOp(this);
   }

   public TSavebp getSavebp() {
      return this._savebp_;
   }

   public void setSavebp(TSavebp node) {
      if (this._savebp_ != null) {
         this._savebp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._savebp_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._savebp_);
   }

   @Override
   void removeChild(Node child) {
      if (this._savebp_ == child) {
         this._savebp_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._savebp_ == oldChild) {
         this.setSavebp((TSavebp)newChild);
      }
   }
}

