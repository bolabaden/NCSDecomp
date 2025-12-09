// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

public final class X2PSubroutine extends XPSubroutine {
   private PSubroutine _pSubroutine_;

   public X2PSubroutine() {
   }

   public X2PSubroutine(PSubroutine _pSubroutine_) {
      this.setPSubroutine(_pSubroutine_);
   }

   @Override
   public Object clone() {
      throw new RuntimeException("Unsupported Operation");
   }

   @Override
   public void apply(Switch sw) {
      throw new RuntimeException("Switch not supported.");
   }

   public PSubroutine getPSubroutine() {
      return this._pSubroutine_;
   }

   public void setPSubroutine(PSubroutine node) {
      if (this._pSubroutine_ != null) {
         this._pSubroutine_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._pSubroutine_ = node;
   }

   @Override
   void removeChild(Node child) {
      if (this._pSubroutine_ == child) {
         this._pSubroutine_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
   }

   @Override
   public String toString() {
      return this.toString(this._pSubroutine_);
   }
}

