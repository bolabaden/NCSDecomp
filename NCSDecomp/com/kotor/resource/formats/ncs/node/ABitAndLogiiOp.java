// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ABitAndLogiiOp extends PLogiiOp {
   private TBoolandii _boolandii_;

   public ABitAndLogiiOp() {
   }

   public ABitAndLogiiOp(TBoolandii _boolandii_) {
      this.setBoolandii(_boolandii_);
   }

   @Override
   public Object clone() {
      return new ABitAndLogiiOp((TBoolandii)this.cloneNode(this._boolandii_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseABitAndLogiiOp(this);
   }

   public TBoolandii getBoolandii() {
      return this._boolandii_;
   }

   public void setBoolandii(TBoolandii node) {
      if (this._boolandii_ != null) {
         this._boolandii_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._boolandii_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._boolandii_);
   }

   @Override
   void removeChild(Node child) {
      if (this._boolandii_ == child) {
         this._boolandii_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._boolandii_ == oldChild) {
         this.setBoolandii((TBoolandii)newChild);
      }
   }
}

