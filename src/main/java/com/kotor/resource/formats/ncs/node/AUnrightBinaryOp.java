// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AUnrightBinaryOp extends PBinaryOp {
   private TUnright _unright_;

   public AUnrightBinaryOp() {
   }

   public AUnrightBinaryOp(TUnright _unright_) {
      this.setUnright(_unright_);
   }

   @Override
   public Object clone() {
      return new AUnrightBinaryOp((TUnright)this.cloneNode(this._unright_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAUnrightBinaryOp(this);
   }

   public TUnright getUnright() {
      return this._unright_;
   }

   public void setUnright(TUnright node) {
      if (this._unright_ != null) {
         this._unright_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._unright_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._unright_);
   }

   @Override
   void removeChild(Node child) {
      if (this._unright_ == child) {
         this._unright_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._unright_ == oldChild) {
         this.setUnright((TUnright)newChild);
      }
   }
}

