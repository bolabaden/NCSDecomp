// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AExclOrLogiiOp extends PLogiiOp {
   private TExcorii _excorii_;

   public AExclOrLogiiOp() {
   }

   public AExclOrLogiiOp(TExcorii _excorii_) {
      this.setExcorii(_excorii_);
   }

   @Override
   public Object clone() {
      return new AExclOrLogiiOp((TExcorii)this.cloneNode(this._excorii_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAExclOrLogiiOp(this);
   }

   public TExcorii getExcorii() {
      return this._excorii_;
   }

   public void setExcorii(TExcorii node) {
      if (this._excorii_ != null) {
         this._excorii_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._excorii_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._excorii_);
   }

   @Override
   void removeChild(Node child) {
      if (this._excorii_ == child) {
         this._excorii_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._excorii_ == oldChild) {
         this.setExcorii((TExcorii)newChild);
      }
   }
}

