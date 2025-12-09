// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ADecispStackOp extends PStackOp {
   private TDecisp _decisp_;

   public ADecispStackOp() {
   }

   public ADecispStackOp(TDecisp _decisp_) {
      this.setDecisp(_decisp_);
   }

   @Override
   public Object clone() {
      return new ADecispStackOp((TDecisp)this.cloneNode(this._decisp_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseADecispStackOp(this);
   }

   public TDecisp getDecisp() {
      return this._decisp_;
   }

   public void setDecisp(TDecisp node) {
      if (this._decisp_ != null) {
         this._decisp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._decisp_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._decisp_);
   }

   @Override
   void removeChild(Node child) {
      if (this._decisp_ == child) {
         this._decisp_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._decisp_ == oldChild) {
         this.setDecisp((TDecisp)newChild);
      }
   }
}

