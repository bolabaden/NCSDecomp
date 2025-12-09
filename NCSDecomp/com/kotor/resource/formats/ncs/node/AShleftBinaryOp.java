// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AShleftBinaryOp extends PBinaryOp {
   private TShleft _shleft_;

   public AShleftBinaryOp() {
   }

   public AShleftBinaryOp(TShleft _shleft_) {
      this.setShleft(_shleft_);
   }

   @Override
   public Object clone() {
      return new AShleftBinaryOp((TShleft)this.cloneNode(this._shleft_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAShleftBinaryOp(this);
   }

   public TShleft getShleft() {
      return this._shleft_;
   }

   public void setShleft(TShleft node) {
      if (this._shleft_ != null) {
         this._shleft_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._shleft_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._shleft_);
   }

   @Override
   void removeChild(Node child) {
      if (this._shleft_ == child) {
         this._shleft_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._shleft_ == oldChild) {
         this.setShleft((TShleft)newChild);
      }
   }
}

