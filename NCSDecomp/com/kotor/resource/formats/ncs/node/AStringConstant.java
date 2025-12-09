// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AStringConstant extends PConstant {
   private TStringLiteral _stringLiteral_;

   public AStringConstant() {
   }

   public AStringConstant(TStringLiteral _stringLiteral_) {
      this.setStringLiteral(_stringLiteral_);
   }

   @Override
   public Object clone() {
      return new AStringConstant((TStringLiteral)this.cloneNode(this._stringLiteral_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAStringConstant(this);
   }

   public TStringLiteral getStringLiteral() {
      return this._stringLiteral_;
   }

   public void setStringLiteral(TStringLiteral node) {
      if (this._stringLiteral_ != null) {
         this._stringLiteral_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._stringLiteral_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._stringLiteral_);
   }

   @Override
   void removeChild(Node child) {
      if (this._stringLiteral_ == child) {
         this._stringLiteral_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._stringLiteral_ == oldChild) {
         this.setStringLiteral((TStringLiteral)newChild);
      }
   }
}

