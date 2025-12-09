// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

public final class X1PCmd extends XPCmd {
   private XPCmd _xPCmd_;
   private PCmd _pCmd_;

   public X1PCmd() {
   }

   public X1PCmd(XPCmd _xPCmd_, PCmd _pCmd_) {
      this.setXPCmd(_xPCmd_);
      this.setPCmd(_pCmd_);
   }

   @Override
   public Object clone() {
      throw new RuntimeException("Unsupported Operation");
   }

   @Override
   public void apply(Switch sw) {
      throw new RuntimeException("Switch not supported.");
   }

   public XPCmd getXPCmd() {
      return this._xPCmd_;
   }

   public void setXPCmd(XPCmd node) {
      if (this._xPCmd_ != null) {
         this._xPCmd_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._xPCmd_ = node;
   }

   public PCmd getPCmd() {
      return this._pCmd_;
   }

   public void setPCmd(PCmd node) {
      if (this._pCmd_ != null) {
         this._pCmd_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._pCmd_ = node;
   }

   @Override
   void removeChild(Node child) {
      if (this._xPCmd_ == child) {
         this._xPCmd_ = null;
      }

      if (this._pCmd_ == child) {
         this._pCmd_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
   }

   @Override
   public String toString() {
      return this.toString(this._xPCmd_) + this.toString(this._pCmd_);
   }
}

