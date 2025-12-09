// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * LinkedList that enforces parent/ownership rules via a Cast hook on inserts.
 */
public class TypedLinkedList<T> extends LinkedList<T> {
   private static final long serialVersionUID = 1L;
   private final Cast<T> cast;

   public TypedLinkedList() {
      this.cast = NoCast.instance();
   }

   public TypedLinkedList(Collection<? extends T> c) {
      this.cast = NoCast.instance();
      this.addAll(c);
   }

   public TypedLinkedList(Cast<T> cast) {
      this.cast = cast;
   }

   public TypedLinkedList(Collection<? extends T> c, Cast<T> cast) {
      this.cast = cast;
      this.addAll(c);
   }

   public Cast<T> getCast() {
      return this.cast;
   }

   @Override
   public T set(int index, T element) {
      return super.set(index, this.cast.cast(element));
   }

   @Override
   public void add(int index, T element) {
      super.add(index, this.cast.cast(element));
   }

   @Override
   public boolean add(T o) {
      return super.add(this.cast.cast(o));
   }

   @Override
   public boolean addAll(Collection<? extends T> c) {
      Iterator<? extends T> i = c.iterator();

      while (i.hasNext()) {
         super.add(this.cast.cast(i.next()));
      }

      return true;
   }

   @Override
   public boolean addAll(int index, Collection<? extends T> c) {
      int pos = index;
      Iterator<? extends T> i = c.iterator();

      while (i.hasNext()) {
         super.add(pos++, this.cast.cast(i.next()));
      }

      return true;
   }

   @Override
   public void addFirst(T o) {
      super.addFirst(this.cast.cast(o));
   }

   @Override
   public void addLast(T o) {
      super.addLast(this.cast.cast(o));
   }

   @Override
   public ListIterator<T> listIterator(int index) {
      return TypedLinkedList.this.new TypedLinkedListIterator(super.listIterator(index));
   }

   private class TypedLinkedListIterator implements ListIterator<T> {
      private final ListIterator<T> iterator;

      TypedLinkedListIterator(ListIterator<T> iterator) {
         this.iterator = iterator;
      }

      @Override
      public boolean hasNext() {
         return this.iterator.hasNext();
      }

      @Override
      public T next() {
         return this.iterator.next();
      }

      @Override
      public boolean hasPrevious() {
         return this.iterator.hasPrevious();
      }

      @Override
      public T previous() {
         return this.iterator.previous();
      }

      @Override
      public int nextIndex() {
         return this.iterator.nextIndex();
      }

      @Override
      public int previousIndex() {
         return this.iterator.previousIndex();
      }

      @Override
      public void remove() {
         this.iterator.remove();
      }

      @Override
      public void set(T o) {
         this.iterator.set(TypedLinkedList.this.cast.cast(o));
      }

      @Override
      public void add(T o) {
         this.iterator.add(TypedLinkedList.this.cast.cast(o));
      }
   }
}

