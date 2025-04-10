package com.minecolonies.api.configuration.builders;

import java.util.function.Consumer;

/**
 * Interface for managing configuration builders.
 */
public interface IConfigBuilder
{
    /**
     * Create a category configuration instance.
     * <p>Allows you to define configuration values listed under a given category.</p>
     *
     * @param key             the category key to use, will determine the translation string.
     * @param categoryBuilder the consumer setting up the category.
     */
    void createCategory(final String key, final Consumer<IConfigCategoryBuilder> categoryBuilder);
}
