package com.minecolonies.api.configuration.builders;

import com.ldtteam.structurize.util.LanguageHandler;
import com.minecolonies.api.util.constant.Constants;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Implementation of the config builder counting as a wrapper around Forge it's {@link ForgeConfigSpec.Builder}.
 * <p>Used to create the Forge config files through the mod loading context.</p>
 */
public class ConfigSpecBuilder implements IConfigBuilder
{
    private final static String INT_DEFAULT_KEY     = "minecolonies.config.default.int";
    private final static String DOUBLE_DEFAULT_KEY  = "minecolonies.config.default.double";
    private final static String LONG_DEFAULT_KEY    = "minecolonies.config.default.long";
    private final static String BOOLEAN_DEFAULT_KEY = "minecolonies.config.default.boolean";

    /**
     * The underlying config spec builder.
     */
    private final ForgeConfigSpec.Builder builder;

    /**
     * Create the config builder from the Forge config builder.
     *
     * @param builder the forge config builder instance.
     */
    public ConfigSpecBuilder(final ForgeConfigSpec.Builder builder)
    {
        this.builder = builder;
    }

    @Override
    public void createCategory(final String key, final Consumer<IConfigCategoryBuilder> categoryBuilder)
    {
        builder.comment(LanguageHandler.translateKey(commentTKey(key))).push(key);
        categoryBuilder.accept(new ConfigSpecCategoryBuilder(builder));
        builder.pop();
    }

    /**
     * Util method to determine the translation key for comments.
     *
     * @param key the config key used in the translation key.
     * @return the translation key.
     */
    private static String commentTKey(final String key)
    {
        return nameTKey(key) + ".comment";
    }

    /**
     * Util method to determine the translation key for names.
     *
     * @param key the config key used in the translation key.
     * @return the translation key.
     */
    private static String nameTKey(final String key)
    {
        return Constants.MOD_ID + ".config." + key;
    }

    /**
     * Underlying category builder implementation.
     */
    private record ConfigSpecCategoryBuilder(ForgeConfigSpec.Builder builder) implements IConfigCategoryBuilder
    {
        @Override
        public ValueHolder<Integer> defineInteger(final String key, final int defaultValue, final int minValue, final int maxValue)
        {
            return buildBase(key, LanguageHandler.translateKeyWithFormat(INT_DEFAULT_KEY, defaultValue, minValue, maxValue)).defineInRange(key,
                defaultValue,
                minValue,
                maxValue)::get;
        }

        @Override
        public ValueHolder<Double> defineDouble(final String key, final double defaultValue, final double minValue, final double maxValue)
        {
            return buildBase(key, LanguageHandler.translateKeyWithFormat(DOUBLE_DEFAULT_KEY, defaultValue, minValue, maxValue)).defineInRange(key,
                defaultValue,
                minValue,
                maxValue)::get;
        }

        @Override
        public ValueHolder<Long> defineLong(final String key, final long defaultValue, final long minValue, final long maxValue)
        {
            return buildBase(key, LanguageHandler.translateKeyWithFormat(LONG_DEFAULT_KEY, defaultValue, minValue, maxValue)).defineInRange(key,
                defaultValue,
                minValue,
                maxValue)::get;
        }

        @Override
        public ValueHolder<Boolean> defineBoolean(final String key, final boolean defaultValue)
        {
            return buildBase(key, LanguageHandler.translateKeyWithFormat(BOOLEAN_DEFAULT_KEY, defaultValue)).define(key, defaultValue)::get;
        }

        @Override
        public <V extends Enum<V>> ValueHolder<V> defineEnum(final String key, final V defaultValue)
        {
            return buildBase(key, "").defineEnum(key, defaultValue)::get;
        }

        @Override
        public <T> ValueHolder<List<? extends T>> defineList(final String key, final List<? extends T> defaultValue, final Predicate<Object> elementValidator)
        {
            return buildBase(key, "").defineList(key, defaultValue, elementValidator)::get;
        }

        private ForgeConfigSpec.Builder buildBase(final String key, final String defaultDesc)
        {
            return builder.comment(LanguageHandler.translateKey(commentTKey(key)) + " " + defaultDesc).translation(nameTKey(key));
        }
    }
}
