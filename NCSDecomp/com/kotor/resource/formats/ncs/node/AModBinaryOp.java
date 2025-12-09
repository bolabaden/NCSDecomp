// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AModBinaryOp extends PBinaryOp {
   private TMod _mod_;

   public AModBinaryOp() {
   }

   public AModBinaryOp(TMod _mod_) {
      this.setMod(_mod_);
   }

   @Override
   public Object clone() {
      return new AModBinaryOp((TMod)this.cloneNode(this._mod_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAModBinaryOp(this);
   }

   public TMod getMod() {
      return this._mod_;
   }

   public void setMod(TMod node) {
      if (this._mod_ != null) {
         this._mod_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._mod_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._mod_);
   }

   @Override
   void removeChild(Node child) {
      if (this._mod_ == child) {
         this._mod_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._mod_ == oldChild) {
         this.setMod((TMod)newChild);
      }
   }
}

