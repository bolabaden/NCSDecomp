// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ALogiiCmd extends PCmd {
   private PLogiiCommand _logiiCommand_;

   public ALogiiCmd() {
   }

   public ALogiiCmd(PLogiiCommand _logiiCommand_) {
      this.setLogiiCommand(_logiiCommand_);
   }

   @Override
   public Object clone() {
      return new ALogiiCmd((PLogiiCommand)this.cloneNode(this._logiiCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseALogiiCmd(this);
   }

   public PLogiiCommand getLogiiCommand() {
      return this._logiiCommand_;
   }

   public void setLogiiCommand(PLogiiCommand node) {
      if (this._logiiCommand_ != null) {
         this._logiiCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._logiiCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._logiiCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._logiiCommand_ == child) {
         this._logiiCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._logiiCommand_ == oldChild) {
         this.setLogiiCommand((PLogiiCommand)newChild);
      }
   }
}

