package com.minecolonies.api.configuration.builders;

import java.util.function.Supplier;

/**
 * Basic interface that holds a reference to a configuration value.
 *
 * @param <T> the type of the value.
 */
@FunctionalInterface
public interface ValueHolder<T> extends Supplier<T>
{
}
