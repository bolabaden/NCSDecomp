// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ANonzeroJumpIf extends PJumpIf {
   private TJnz _jnz_;

   public ANonzeroJumpIf() {
   }

   public ANonzeroJumpIf(TJnz _jnz_) {
      this.setJnz(_jnz_);
   }

   @Override
   public Object clone() {
      return new ANonzeroJumpIf((TJnz)this.cloneNode(this._jnz_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseANonzeroJumpIf(this);
   }

   public TJnz getJnz() {
      return this._jnz_;
   }

   public void setJnz(TJnz node) {
      if (this._jnz_ != null) {
         this._jnz_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._jnz_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._jnz_);
   }

   @Override
   void removeChild(Node child) {
      if (this._jnz_ == child) {
         this._jnz_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._jnz_ == oldChild) {
         this.setJnz((TJnz)newChild);
      }
   }
}

