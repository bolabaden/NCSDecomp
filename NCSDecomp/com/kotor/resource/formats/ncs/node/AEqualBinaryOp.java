// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AEqualBinaryOp extends PBinaryOp {
   private TEqual _equal_;

   public AEqualBinaryOp() {
   }

   public AEqualBinaryOp(TEqual _equal_) {
      this.setEqual(_equal_);
   }

   @Override
   public Object clone() {
      return new AEqualBinaryOp((TEqual)this.cloneNode(this._equal_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAEqualBinaryOp(this);
   }

   public TEqual getEqual() {
      return this._equal_;
   }

   public void setEqual(TEqual node) {
      if (this._equal_ != null) {
         this._equal_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._equal_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._equal_);
   }

   @Override
   void removeChild(Node child) {
      if (this._equal_ == child) {
         this._equal_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._equal_ == oldChild) {
         this.setEqual((TEqual)newChild);
      }
   }
}

