// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AAndLogiiOp extends PLogiiOp {
   private TLogandii _logandii_;

   public AAndLogiiOp() {
   }

   public AAndLogiiOp(TLogandii _logandii_) {
      this.setLogandii(_logandii_);
   }

   @Override
   public Object clone() {
      return new AAndLogiiOp((TLogandii)this.cloneNode(this._logandii_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAAndLogiiOp(this);
   }

   public TLogandii getLogandii() {
      return this._logandii_;
   }

   public void setLogandii(TLogandii node) {
      if (this._logandii_ != null) {
         this._logandii_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._logandii_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._logandii_);
   }

   @Override
   void removeChild(Node child) {
      if (this._logandii_ == child) {
         this._logandii_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._logandii_ == oldChild) {
         this.setLogandii((TLogandii)newChild);
      }
   }
}

