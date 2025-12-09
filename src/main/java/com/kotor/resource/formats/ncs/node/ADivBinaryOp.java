// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ADivBinaryOp extends PBinaryOp {
   private TDiv _div_;

   public ADivBinaryOp() {
   }

   public ADivBinaryOp(TDiv _div_) {
      this.setDiv(_div_);
   }

   @Override
   public Object clone() {
      return new ADivBinaryOp((TDiv)this.cloneNode(this._div_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseADivBinaryOp(this);
   }

   public TDiv getDiv() {
      return this._div_;
   }

   public void setDiv(TDiv node) {
      if (this._div_ != null) {
         this._div_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._div_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._div_);
   }

   @Override
   void removeChild(Node child) {
      if (this._div_ == child) {
         this._div_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._div_ == oldChild) {
         this.setDiv((TDiv)newChild);
      }
   }
}

