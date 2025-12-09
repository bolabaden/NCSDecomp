// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AIncispStackOp extends PStackOp {
   private TIncisp _incisp_;

   public AIncispStackOp() {
   }

   public AIncispStackOp(TIncisp _incisp_) {
      this.setIncisp(_incisp_);
   }

   @Override
   public Object clone() {
      return new AIncispStackOp((TIncisp)this.cloneNode(this._incisp_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAIncispStackOp(this);
   }

   public TIncisp getIncisp() {
      return this._incisp_;
   }

   public void setIncisp(TIncisp node) {
      if (this._incisp_ != null) {
         this._incisp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._incisp_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._incisp_);
   }

   @Override
   void removeChild(Node child) {
      if (this._incisp_ == child) {
         this._incisp_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._incisp_ == oldChild) {
         this.setIncisp((TIncisp)newChild);
      }
   }
}

