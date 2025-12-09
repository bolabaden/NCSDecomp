// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

public final class X2PCmd extends XPCmd {
   private PCmd _pCmd_;

   public X2PCmd() {
   }

   public X2PCmd(PCmd _pCmd_) {
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
      if (this._pCmd_ == child) {
         this._pCmd_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
   }

   @Override
   public String toString() {
      return this.toString(this._pCmd_);
   }
}

