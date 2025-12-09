// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ACopyTopBpCommand extends PCopyTopBpCommand {
   private TCptopbp _cptopbp_;
   private TIntegerConstant _pos_;
   private TIntegerConstant _type_;
   private TIntegerConstant _offset_;
   private TIntegerConstant _size_;
   private TSemi _semi_;

   public ACopyTopBpCommand() {
   }

   public ACopyTopBpCommand(
      TCptopbp _cptopbp_, TIntegerConstant _pos_, TIntegerConstant _type_, TIntegerConstant _offset_, TIntegerConstant _size_, TSemi _semi_
   ) {
      this.setCptopbp(_cptopbp_);
      this.setPos(_pos_);
      this.setType(_type_);
      this.setOffset(_offset_);
      this.setSize(_size_);
      this.setSemi(_semi_);
   }

   @Override
   public Object clone() {
      return new ACopyTopBpCommand(
         (TCptopbp)this.cloneNode(this._cptopbp_),
         (TIntegerConstant)this.cloneNode(this._pos_),
         (TIntegerConstant)this.cloneNode(this._type_),
         (TIntegerConstant)this.cloneNode(this._offset_),
         (TIntegerConstant)this.cloneNode(this._size_),
         (TSemi)this.cloneNode(this._semi_)
      );
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACopyTopBpCommand(this);
   }

   public TCptopbp getCptopbp() {
      return this._cptopbp_;
   }

   public void setCptopbp(TCptopbp node) {
      if (this._cptopbp_ != null) {
         this._cptopbp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._cptopbp_ = node;
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

   public TIntegerConstant getOffset() {
      return this._offset_;
   }

   public void setOffset(TIntegerConstant node) {
      if (this._offset_ != null) {
         this._offset_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._offset_ = node;
   }

   public TIntegerConstant getSize() {
      return this._size_;
   }

   public void setSize(TIntegerConstant node) {
      if (this._size_ != null) {
         this._size_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._size_ = node;
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
      return this.toString(this._cptopbp_)
         + this.toString(this._pos_)
         + this.toString(this._type_)
         + this.toString(this._offset_)
         + this.toString(this._size_)
         + this.toString(this._semi_);
   }

   @Override
   void removeChild(Node child) {
      if (this._cptopbp_ == child) {
         this._cptopbp_ = null;
      } else if (this._pos_ == child) {
         this._pos_ = null;
      } else if (this._type_ == child) {
         this._type_ = null;
      } else if (this._offset_ == child) {
         this._offset_ = null;
      } else if (this._size_ == child) {
         this._size_ = null;
      } else if (this._semi_ == child) {
         this._semi_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._cptopbp_ == oldChild) {
         this.setCptopbp((TCptopbp)newChild);
      } else if (this._pos_ == oldChild) {
         this.setPos((TIntegerConstant)newChild);
      } else if (this._type_ == oldChild) {
         this.setType((TIntegerConstant)newChild);
      } else if (this._offset_ == oldChild) {
         this.setOffset((TIntegerConstant)newChild);
      } else if (this._size_ == oldChild) {
         this.setSize((TIntegerConstant)newChild);
      } else if (this._semi_ == oldChild) {
         this.setSemi((TSemi)newChild);
      }
   }
}

