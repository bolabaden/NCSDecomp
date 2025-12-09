// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ACopydownbpCmd extends PCmd {
   private PCopyDownBpCommand _copyDownBpCommand_;

   public ACopydownbpCmd() {
   }

   public ACopydownbpCmd(PCopyDownBpCommand _copyDownBpCommand_) {
      this.setCopyDownBpCommand(_copyDownBpCommand_);
   }

   @Override
   public Object clone() {
      return new ACopydownbpCmd((PCopyDownBpCommand)this.cloneNode(this._copyDownBpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACopydownbpCmd(this);
   }

   public PCopyDownBpCommand getCopyDownBpCommand() {
      return this._copyDownBpCommand_;
   }

   public void setCopyDownBpCommand(PCopyDownBpCommand node) {
      if (this._copyDownBpCommand_ != null) {
         this._copyDownBpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._copyDownBpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._copyDownBpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._copyDownBpCommand_ == child) {
         this._copyDownBpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._copyDownBpCommand_ == oldChild) {
         this.setCopyDownBpCommand((PCopyDownBpCommand)newChild);
      }
   }
}

