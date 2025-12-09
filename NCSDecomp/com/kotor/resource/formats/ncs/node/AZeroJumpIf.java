// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AZeroJumpIf extends PJumpIf {
   private TJz _jz_;

   public AZeroJumpIf() {
   }

   public AZeroJumpIf(TJz _jz_) {
      this.setJz(_jz_);
   }

   @Override
   public Object clone() {
      return new AZeroJumpIf((TJz)this.cloneNode(this._jz_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAZeroJumpIf(this);
   }

   public TJz getJz() {
      return this._jz_;
   }

   public void setJz(TJz node) {
      if (this._jz_ != null) {
         this._jz_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._jz_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._jz_);
   }

   @Override
   void removeChild(Node child) {
      if (this._jz_ == child) {
         this._jz_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._jz_ == oldChild) {
         this.setJz((TJz)newChild);
      }
   }
}

