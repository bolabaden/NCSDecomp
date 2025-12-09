// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

/**
 * AST node for the 'greater-than' binary operator.
 */
public final class AGtBinaryOp extends PBinaryOp {
   private TGt _gt_;

   public AGtBinaryOp() {
   }

   public AGtBinaryOp(TGt _gt_) {
      this.setGt(_gt_);
   }

   @Override
   public Object clone() {
      return new AGtBinaryOp((TGt)this.cloneNode(this._gt_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAGtBinaryOp(this);
   }

   public TGt getGt() {
      return this._gt_;
   }

   public void setGt(TGt node) {
      if (this._gt_ != null) {
         this._gt_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._gt_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._gt_);
   }

   @Override
   void removeChild(Node child) {
      if (this._gt_ == child) {
         this._gt_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._gt_ == oldChild) {
         this.setGt((TGt)newChild);
      }
   }
}

