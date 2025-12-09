// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AGeqBinaryOp extends PBinaryOp {
   private TGeq _geq_;

   public AGeqBinaryOp() {
   }

   public AGeqBinaryOp(TGeq _geq_) {
      this.setGeq(_geq_);
   }

   @Override
   public Object clone() {
      return new AGeqBinaryOp((TGeq)this.cloneNode(this._geq_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAGeqBinaryOp(this);
   }

   public TGeq getGeq() {
      return this._geq_;
   }

   public void setGeq(TGeq node) {
      if (this._geq_ != null) {
         this._geq_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._geq_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._geq_);
   }

   @Override
   void removeChild(Node child) {
      if (this._geq_ == child) {
         this._geq_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._geq_ == oldChild) {
         this.setGeq((TGeq)newChild);
      }
   }
}

