// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AShrightBinaryOp extends PBinaryOp {
   private TShright _shright_;

   public AShrightBinaryOp() {
   }

   public AShrightBinaryOp(TShright _shright_) {
      this.setShright(_shright_);
   }

   @Override
   public Object clone() {
      return new AShrightBinaryOp((TShright)this.cloneNode(this._shright_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAShrightBinaryOp(this);
   }

   public TShright getShright() {
      return this._shright_;
   }

   public void setShright(TShright node) {
      if (this._shright_ != null) {
         this._shright_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._shright_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._shright_);
   }

   @Override
   void removeChild(Node child) {
      if (this._shright_ == child) {
         this._shright_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._shright_ == oldChild) {
         this.setShright((TShright)newChild);
      }
   }
}

