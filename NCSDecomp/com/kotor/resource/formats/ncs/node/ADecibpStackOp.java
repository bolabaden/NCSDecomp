// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ADecibpStackOp extends PStackOp {
   private TDecibp _decibp_;

   public ADecibpStackOp() {
   }

   public ADecibpStackOp(TDecibp _decibp_) {
      this.setDecibp(_decibp_);
   }

   @Override
   public Object clone() {
      return new ADecibpStackOp((TDecibp)this.cloneNode(this._decibp_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseADecibpStackOp(this);
   }

   public TDecibp getDecibp() {
      return this._decibp_;
   }

   public void setDecibp(TDecibp node) {
      if (this._decibp_ != null) {
         this._decibp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._decibp_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._decibp_);
   }

   @Override
   void removeChild(Node child) {
      if (this._decibp_ == child) {
         this._decibp_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._decibp_ == oldChild) {
         this.setDecibp((TDecibp)newChild);
      }
   }
}

