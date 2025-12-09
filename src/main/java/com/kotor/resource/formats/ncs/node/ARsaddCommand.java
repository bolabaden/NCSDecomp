// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ARsaddCommand extends PRsaddCommand {
   private TRsadd _rsadd_;
   private TIntegerConstant _pos_;
   private TIntegerConstant _type_;
   private TSemi _semi_;

   public ARsaddCommand() {
   }

   public ARsaddCommand(TRsadd _rsadd_, TIntegerConstant _pos_, TIntegerConstant _type_, TSemi _semi_) {
      this.setRsadd(_rsadd_);
      this.setPos(_pos_);
      this.setType(_type_);
      this.setSemi(_semi_);
   }

   @Override
   public Object clone() {
      return new ARsaddCommand(
         (TRsadd)this.cloneNode(this._rsadd_),
         (TIntegerConstant)this.cloneNode(this._pos_),
         (TIntegerConstant)this.cloneNode(this._type_),
         (TSemi)this.cloneNode(this._semi_)
      );
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseARsaddCommand(this);
   }

   public TRsadd getRsadd() {
      return this._rsadd_;
   }

   public void setRsadd(TRsadd node) {
      if (this._rsadd_ != null) {
         this._rsadd_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._rsadd_ = node;
   }

   public TIntegerConstant getPos() {
      return this._pos_;
   }

   public void setPos(TIntegerConstant node) {
      if (this._pos_ != null) {
         this._pos_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._pos_ = node;
   }

   public TIntegerConstant getType() {
      return this._type_;
   }

   public void setType(TIntegerConstant node) {
      if (this._type_ != null) {
         this._type_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._type_ = node;
   }

   public TSemi getSemi() {
      return this._semi_;
   }

   public void setSemi(TSemi node) {
      if (this._semi_ != null) {
         this._semi_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._semi_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._rsadd_) + this.toString(this._pos_) + this.toString(this._type_) + this.toString(this._semi_);
   }

   @Override
   void removeChild(Node child) {
      if (this._rsadd_ == child) {
         this._rsadd_ = null;
      } else if (this._pos_ == child) {
         this._pos_ = null;
      } else if (this._type_ == child) {
         this._type_ = null;
      } else if (this._semi_ == child) {
         this._semi_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._rsadd_ == oldChild) {
         this.setRsadd((TRsadd)newChild);
      } else if (this._pos_ == oldChild) {
         this.setPos((TIntegerConstant)newChild);
      } else if (this._type_ == oldChild) {
         this.setType((TIntegerConstant)newChild);
      } else if (this._semi_ == oldChild) {
         this.setSemi((TSemi)newChild);
      }
   }
}

