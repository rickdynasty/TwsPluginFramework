package com.animaconnected.future;

import java.io.IOException;

public interface MapCallback<T, D> {

    D onResult(T result) throws IOException;
}
