package org.partiql.eval;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Supplier;

public class IterableFromIteratorSupplier<T> implements Iterable<T>{
    @NotNull
    final Supplier<Iterator<T>> _supplier;

    public IterableFromIteratorSupplier(@NotNull Supplier<Iterator<T>> supplier) {
        _supplier = supplier;
    }

    @Override
    public Iterator<T> iterator() {
        return _supplier.get();
    }
}
