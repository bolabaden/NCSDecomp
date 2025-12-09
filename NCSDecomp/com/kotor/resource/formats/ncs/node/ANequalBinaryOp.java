// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ANequalBinaryOp extends PBinaryOp {
   private TNequal _nequal_;

   public ANequalBinaryOp() {
   }

   public ANequalBinaryOp(TNequal _nequal_) {
      this.setNequal(_nequal_);
   }

   @Override
   public Object clone() {
      return new ANequalBinaryOp((TNequal)this.cloneNode(this._nequal_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseANequalBinaryOp(this);
   }

   public TNequal getNequal() {
      return this._nequal_;
   }

   public void setNequal(TNequal node) {
      if (this._nequal_ != null) {
         this._nequal_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._nequal_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._nequal_);
   }

   @Override
   void removeChild(Node child) {
      if (this._nequal_ == child) {
         this._nequal_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._nequal_ == oldChild) {
         this.setNequal((TNequal)newChild);
      }
   }
}

