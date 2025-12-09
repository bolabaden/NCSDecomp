// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ALeqBinaryOp extends PBinaryOp {
   private TLeq _leq_;

   public ALeqBinaryOp() {
   }

   public ALeqBinaryOp(TLeq _leq_) {
      this.setLeq(_leq_);
   }

   @Override
   public Object clone() {
      return new ALeqBinaryOp((TLeq)this.cloneNode(this._leq_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseALeqBinaryOp(this);
   }

   public TLeq getLeq() {
      return this._leq_;
   }

   public void setLeq(TLeq node) {
      if (this._leq_ != null) {
         this._leq_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._leq_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._leq_);
   }

   @Override
   void removeChild(Node child) {
      if (this._leq_ == child) {
         this._leq_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._leq_ == oldChild) {
         this.setLeq((TLeq)newChild);
      }
   }
}

