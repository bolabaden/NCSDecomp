// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AMulBinaryOp extends PBinaryOp {
   private TMul _mul_;

   public AMulBinaryOp() {
   }

   public AMulBinaryOp(TMul _mul_) {
      this.setMul(_mul_);
   }

   @Override
   public Object clone() {
      return new AMulBinaryOp((TMul)this.cloneNode(this._mul_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAMulBinaryOp(this);
   }

   public TMul getMul() {
      return this._mul_;
   }

   public void setMul(TMul node) {
      if (this._mul_ != null) {
         this._mul_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._mul_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._mul_);
   }

   @Override
   void removeChild(Node child) {
      if (this._mul_ == child) {
         this._mul_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._mul_ == oldChild) {
         this.setMul((TMul)newChild);
      }
   }
}

