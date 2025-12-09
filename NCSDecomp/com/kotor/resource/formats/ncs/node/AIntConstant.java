// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AIntConstant extends PConstant {
   private TIntegerConstant _integerConstant_;

   public AIntConstant() {
   }

   public AIntConstant(TIntegerConstant _integerConstant_) {
      this.setIntegerConstant(_integerConstant_);
   }

   @Override
   public Object clone() {
      return new AIntConstant((TIntegerConstant)this.cloneNode(this._integerConstant_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAIntConstant(this);
   }

   public TIntegerConstant getIntegerConstant() {
      return this._integerConstant_;
   }

   public void setIntegerConstant(TIntegerConstant node) {
      if (this._integerConstant_ != null) {
         this._integerConstant_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._integerConstant_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._integerConstant_);
   }

   @Override
   void removeChild(Node child) {
      if (this._integerConstant_ == child) {
         this._integerConstant_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._integerConstant_ == oldChild) {
         this.setIntegerConstant((TIntegerConstant)newChild);
      }
   }
}

