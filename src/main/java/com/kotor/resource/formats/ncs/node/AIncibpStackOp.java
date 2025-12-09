// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AIncibpStackOp extends PStackOp {
   private TIncibp _incibp_;

   public AIncibpStackOp() {
   }

   public AIncibpStackOp(TIncibp _incibp_) {
      this.setIncibp(_incibp_);
   }

   @Override
   public Object clone() {
      return new AIncibpStackOp((TIncibp)this.cloneNode(this._incibp_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAIncibpStackOp(this);
   }

   public TIncibp getIncibp() {
      return this._incibp_;
   }

   public void setIncibp(TIncibp node) {
      if (this._incibp_ != null) {
         this._incibp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._incibp_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._incibp_);
   }

   @Override
   void removeChild(Node child) {
      if (this._incibp_ == child) {
         this._incibp_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._incibp_ == oldChild) {
         this.setIncibp((TIncibp)newChild);
      }
   }
}

