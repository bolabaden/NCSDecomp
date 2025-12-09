// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class Start extends Node {
   private PProgram _pProgram_;
   private EOF _eof_;

   public Start() {
   }

   public Start(PProgram _pProgram_, EOF _eof_) {
      this.setPProgram(_pProgram_);
      this.setEOF(_eof_);
   }

   @Override
   public Object clone() {
      return new Start((PProgram)this.cloneNode(this._pProgram_), (EOF)this.cloneNode(this._eof_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseStart(this);
   }

   public PProgram getPProgram() {
      return this._pProgram_;
   }

   public void setPProgram(PProgram node) {
      if (this._pProgram_ != null) {
         this._pProgram_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._pProgram_ = node;
   }

   public EOF getEOF() {
      return this._eof_;
   }

   public void setEOF(EOF node) {
      if (this._eof_ != null) {
         this._eof_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._eof_ = node;
   }

   @Override
   void removeChild(Node child) {
      if (this._pProgram_ == child) {
         this._pProgram_ = null;
      } else if (this._eof_ == child) {
         this._eof_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._pProgram_ == oldChild) {
         this.setPProgram((PProgram)newChild);
      } else if (this._eof_ == oldChild) {
         this.setEOF((EOF)newChild);
      }
   }

   @Override
   public String toString() {
      return this.toString(this._pProgram_) + this.toString(this._eof_);
   }
}

