// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AInclOrLogiiOp extends PLogiiOp {
   private TIncorii _incorii_;

   public AInclOrLogiiOp() {
   }

   public AInclOrLogiiOp(TIncorii _incorii_) {
      this.setIncorii(_incorii_);
   }

   @Override
   public Object clone() {
      return new AInclOrLogiiOp((TIncorii)this.cloneNode(this._incorii_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAInclOrLogiiOp(this);
   }

   public TIncorii getIncorii() {
      return this._incorii_;
   }

   public void setIncorii(TIncorii node) {
      if (this._incorii_ != null) {
         this._incorii_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._incorii_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._incorii_);
   }

   @Override
   void removeChild(Node child) {
      if (this._incorii_ == child) {
         this._incorii_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._incorii_ == oldChild) {
         this.setIncorii((TIncorii)newChild);
      }
   }
}

