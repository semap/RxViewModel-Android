package semap.rx.viewmodel;

import androidx.annotation.Nullable;

import java.util.NoSuchElementException;

public class Optional<M> {

    private final M optional;

    public Optional(@Nullable M optional) {
        this.optional = optional;
    }

    public boolean isEmpty() {
        return this.optional == null;
    }

    public M get() {
        if (optional == null) {
            throw new NoSuchElementException("No value present");
        }
        return optional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Optional<M> optional1 = (Optional<M>) o;
        if (isEmpty()) {
            return optional1.isEmpty();
        }

        return optional.equals(optional1.optional);
    }

}