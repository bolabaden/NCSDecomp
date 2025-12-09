// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ACopytopspCmd extends PCmd {
   private PCopyTopSpCommand _copyTopSpCommand_;

   public ACopytopspCmd() {
   }

   public ACopytopspCmd(PCopyTopSpCommand _copyTopSpCommand_) {
      this.setCopyTopSpCommand(_copyTopSpCommand_);
   }

   @Override
   public Object clone() {
      return new ACopytopspCmd((PCopyTopSpCommand)this.cloneNode(this._copyTopSpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACopytopspCmd(this);
   }

   public PCopyTopSpCommand getCopyTopSpCommand() {
      return this._copyTopSpCommand_;
   }

   public void setCopyTopSpCommand(PCopyTopSpCommand node) {
      if (this._copyTopSpCommand_ != null) {
         this._copyTopSpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._copyTopSpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._copyTopSpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._copyTopSpCommand_ == child) {
         this._copyTopSpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._copyTopSpCommand_ == oldChild) {
         this.setCopyTopSpCommand((PCopyTopSpCommand)newChild);
      }
   }
}

