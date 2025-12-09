// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

public final class X1PSubroutine extends XPSubroutine {
   private XPSubroutine _xPSubroutine_;
   private PSubroutine _pSubroutine_;

   public X1PSubroutine() {
   }

   public X1PSubroutine(XPSubroutine _xPSubroutine_, PSubroutine _pSubroutine_) {
      this.setXPSubroutine(_xPSubroutine_);
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

   public XPSubroutine getXPSubroutine() {
      return this._xPSubroutine_;
   }

   public void setXPSubroutine(XPSubroutine node) {
      if (this._xPSubroutine_ != null) {
         this._xPSubroutine_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._xPSubroutine_ = node;
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
      if (this._xPSubroutine_ == child) {
         this._xPSubroutine_ = null;
      }

      if (this._pSubroutine_ == child) {
         this._pSubroutine_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
   }

   @Override
   public String toString() {
      return this.toString(this._xPSubroutine_) + this.toString(this._pSubroutine_);
   }
}

