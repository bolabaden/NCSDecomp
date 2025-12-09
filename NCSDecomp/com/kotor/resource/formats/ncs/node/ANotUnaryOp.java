// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ANotUnaryOp extends PUnaryOp {
   private TNot _not_;

   public ANotUnaryOp() {
   }

   public ANotUnaryOp(TNot _not_) {
      this.setNot(_not_);
   }

   @Override
   public Object clone() {
      return new ANotUnaryOp((TNot)this.cloneNode(this._not_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseANotUnaryOp(this);
   }

   public TNot getNot() {
      return this._not_;
   }

   public void setNot(TNot node) {
      if (this._not_ != null) {
         this._not_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._not_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._not_);
   }

   @Override
   void removeChild(Node child) {
      if (this._not_ == child) {
         this._not_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._not_ == oldChild) {
         this.setNot((TNot)newChild);
      }
   }
}

