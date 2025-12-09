// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AOrLogiiOp extends PLogiiOp {
   private TLogorii _logorii_;

   public AOrLogiiOp() {
   }

   public AOrLogiiOp(TLogorii _logorii_) {
      this.setLogorii(_logorii_);
   }

   @Override
   public Object clone() {
      return new AOrLogiiOp((TLogorii)this.cloneNode(this._logorii_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAOrLogiiOp(this);
   }

   public TLogorii getLogorii() {
      return this._logorii_;
   }

   public void setLogorii(TLogorii node) {
      if (this._logorii_ != null) {
         this._logorii_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._logorii_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._logorii_);
   }

   @Override
   void removeChild(Node child) {
      if (this._logorii_ == child) {
         this._logorii_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._logorii_ == oldChild) {
         this.setLogorii((TLogorii)newChild);
      }
   }
}

