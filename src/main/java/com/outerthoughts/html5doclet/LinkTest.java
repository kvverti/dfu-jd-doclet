package com.outerthoughts.javadoc.iframed;

import java.util.List;
import java.util.function.Function;

public interface LinkTest<A extends Number, B> {

    <B1> LinkTest<A, B1> trans1(Function<? super B, ? extends B1> mapper);
    <A1 extends Number, B1 extends A1> void trans2(LinkTest<A1, B1> thing);
    <A1 extends LinkTest<A, A1>> A1 trans3(List<? extends LinkTest<A, A1>> ls);
}
