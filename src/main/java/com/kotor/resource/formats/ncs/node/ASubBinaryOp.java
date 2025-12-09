// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ASubBinaryOp extends PBinaryOp {
   private TSub _sub_;

   public ASubBinaryOp() {
   }

   public ASubBinaryOp(TSub _sub_) {
      this.setSub(_sub_);
   }

   @Override
   public Object clone() {
      return new ASubBinaryOp((TSub)this.cloneNode(this._sub_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseASubBinaryOp(this);
   }

   public TSub getSub() {
      return this._sub_;
   }

   public void setSub(TSub node) {
      if (this._sub_ != null) {
         this._sub_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._sub_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._sub_);
   }

   @Override
   void removeChild(Node child) {
      if (this._sub_ == child) {
         this._sub_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._sub_ == oldChild) {
         this.setSub((TSub)newChild);
      }
   }
}

