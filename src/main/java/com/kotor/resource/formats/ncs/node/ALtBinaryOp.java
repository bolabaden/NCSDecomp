// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ALtBinaryOp extends PBinaryOp {
   private TLt _lt_;

   public ALtBinaryOp() {
   }

   public ALtBinaryOp(TLt _lt_) {
      this.setLt(_lt_);
   }

   @Override
   public Object clone() {
      return new ALtBinaryOp((TLt)this.cloneNode(this._lt_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseALtBinaryOp(this);
   }

   public TLt getLt() {
      return this._lt_;
   }

   public void setLt(TLt node) {
      if (this._lt_ != null) {
         this._lt_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._lt_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._lt_);
   }

   @Override
   void removeChild(Node child) {
      if (this._lt_ == child) {
         this._lt_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._lt_ == oldChild) {
         this.setLt((TLt)newChild);
      }
   }
}

