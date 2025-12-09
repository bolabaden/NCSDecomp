// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ACopytopbpCmd extends PCmd {
   private PCopyTopBpCommand _copyTopBpCommand_;

   public ACopytopbpCmd() {
   }

   public ACopytopbpCmd(PCopyTopBpCommand _copyTopBpCommand_) {
      this.setCopyTopBpCommand(_copyTopBpCommand_);
   }

   @Override
   public Object clone() {
      return new ACopytopbpCmd((PCopyTopBpCommand)this.cloneNode(this._copyTopBpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACopytopbpCmd(this);
   }

   public PCopyTopBpCommand getCopyTopBpCommand() {
      return this._copyTopBpCommand_;
   }

   public void setCopyTopBpCommand(PCopyTopBpCommand node) {
      if (this._copyTopBpCommand_ != null) {
         this._copyTopBpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._copyTopBpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._copyTopBpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._copyTopBpCommand_ == child) {
         this._copyTopBpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._copyTopBpCommand_ == oldChild) {
         this.setCopyTopBpCommand((PCopyTopBpCommand)newChild);
      }
   }
}

