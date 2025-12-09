// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ACompUnaryOp extends PUnaryOp {
   private TComp _comp_;

   public ACompUnaryOp() {
   }

   public ACompUnaryOp(TComp _comp_) {
      this.setComp(_comp_);
   }

   @Override
   public Object clone() {
      return new ACompUnaryOp((TComp)this.cloneNode(this._comp_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACompUnaryOp(this);
   }

   public TComp getComp() {
      return this._comp_;
   }

   public void setComp(TComp node) {
      if (this._comp_ != null) {
         this._comp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._comp_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._comp_);
   }

   @Override
   void removeChild(Node child) {
      if (this._comp_ == child) {
         this._comp_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._comp_ == oldChild) {
         this.setComp((TComp)newChild);
      }
   }
}

