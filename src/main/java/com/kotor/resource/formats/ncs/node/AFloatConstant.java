// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AFloatConstant extends PConstant {
   private TFloatConstant _floatConstant_;

   public AFloatConstant() {
   }

   public AFloatConstant(TFloatConstant _floatConstant_) {
      this.setFloatConstant(_floatConstant_);
   }

   @Override
   public Object clone() {
      return new AFloatConstant((TFloatConstant)this.cloneNode(this._floatConstant_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAFloatConstant(this);
   }

   public TFloatConstant getFloatConstant() {
      return this._floatConstant_;
   }

   public void setFloatConstant(TFloatConstant node) {
      if (this._floatConstant_ != null) {
         this._floatConstant_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._floatConstant_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._floatConstant_);
   }

   @Override
   void removeChild(Node child) {
      if (this._floatConstant_ == child) {
         this._floatConstant_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._floatConstant_ == oldChild) {
         this.setFloatConstant((TFloatConstant)newChild);
      }
   }
}

