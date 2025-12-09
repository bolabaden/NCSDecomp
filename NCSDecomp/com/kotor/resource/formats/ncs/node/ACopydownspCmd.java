// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ACopydownspCmd extends PCmd {
   private PCopyDownSpCommand _copyDownSpCommand_;

   public ACopydownspCmd() {
   }

   public ACopydownspCmd(PCopyDownSpCommand _copyDownSpCommand_) {
      this.setCopyDownSpCommand(_copyDownSpCommand_);
   }

   @Override
   public Object clone() {
      return new ACopydownspCmd((PCopyDownSpCommand)this.cloneNode(this._copyDownSpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACopydownspCmd(this);
   }

   public PCopyDownSpCommand getCopyDownSpCommand() {
      return this._copyDownSpCommand_;
   }

   public void setCopyDownSpCommand(PCopyDownSpCommand node) {
      if (this._copyDownSpCommand_ != null) {
         this._copyDownSpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._copyDownSpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._copyDownSpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._copyDownSpCommand_ == child) {
         this._copyDownSpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._copyDownSpCommand_ == oldChild) {
         this.setCopyDownSpCommand((PCopyDownSpCommand)newChild);
      }
   }
}

