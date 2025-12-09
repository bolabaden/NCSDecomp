// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ARestorebpBpOp extends PBpOp {
   private TRestorebp _restorebp_;

   public ARestorebpBpOp() {
   }

   public ARestorebpBpOp(TRestorebp _restorebp_) {
      this.setRestorebp(_restorebp_);
   }

   @Override
   public Object clone() {
      return new ARestorebpBpOp((TRestorebp)this.cloneNode(this._restorebp_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseARestorebpBpOp(this);
   }

   public TRestorebp getRestorebp() {
      return this._restorebp_;
   }

   public void setRestorebp(TRestorebp node) {
      if (this._restorebp_ != null) {
         this._restorebp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._restorebp_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._restorebp_);
   }

   @Override
   void removeChild(Node child) {
      if (this._restorebp_ == child) {
         this._restorebp_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._restorebp_ == oldChild) {
         this.setRestorebp((TRestorebp)newChild);
      }
   }
}

