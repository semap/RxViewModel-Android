package semap.rx.viewmodel;

public interface StateMapper<S> {
    S map(S oldState);
}