// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AAddBinaryOp extends PBinaryOp {
   private TAdd _add_;

   public AAddBinaryOp() {
   }

   public AAddBinaryOp(TAdd _add_) {
      this.setAdd(_add_);
   }

   @Override
   public Object clone() {
      return new AAddBinaryOp((TAdd)this.cloneNode(this._add_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAAddBinaryOp(this);
   }

   public TAdd getAdd() {
      return this._add_;
   }

   public void setAdd(TAdd node) {
      if (this._add_ != null) {
         this._add_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._add_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._add_);
   }

   @Override
   void removeChild(Node child) {
      if (this._add_ == child) {
         this._add_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._add_ == oldChild) {
         this.setAdd((TAdd)newChild);
      }
   }
}

