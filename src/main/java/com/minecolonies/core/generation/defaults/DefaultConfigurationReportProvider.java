package com.minecolonies.core.generation.defaults;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ldtteam.structurize.util.LanguageHandler;
import com.minecolonies.api.configuration.ClientConfiguration;
import com.minecolonies.api.configuration.CommonConfiguration;
import com.minecolonies.api.configuration.ServerConfiguration;
import com.minecolonies.api.configuration.builders.IConfigBuilder;
import com.minecolonies.api.configuration.builders.IConfigCategoryBuilder;
import com.minecolonies.api.configuration.builders.ValueHolder;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DefaultConfigurationReportProvider implements DataProvider
{
    protected final PackOutput packOutput;

    public DefaultConfigurationReportProvider(@NotNull final PackOutput packOutput)
    {
        this.packOutput = packOutput;
    }

    @Override
    @NotNull
    public CompletableFuture<?> run(final @NotNull CachedOutput cache)
    {
        final JsonObject root = new JsonObject();
        root.add("common", writeConfigurationInstance(CommonConfiguration::new));
        root.add("server", writeConfigurationInstance(ServerConfiguration::new));
        root.add("client", writeConfigurationInstance(ClientConfiguration::new));

        return DataProvider.saveStable(cache, root, this.packOutput.getOutputFolder(PackOutput.Target.REPORTS).resolve(Constants.MOD_ID).resolve("config.json"));
    }

    @Override
    @NotNull
    public String getName()
    {
        return "Default Config Report Provider";
    }

    private JsonObject writeConfigurationInstance(final Consumer<IConfigBuilder> consumer)
    {
        final Map<String, JsonArray> optionsPerCategory = new HashMap<>();

        consumer.accept(new ConfigJsonBuilder((category, key, type, option) -> {
            option.addProperty("key", key);
            option.addProperty("name", LanguageHandler.translateKey(Constants.MOD_ID + ".config." + key));
            option.addProperty("description", LanguageHandler.translateKey(Constants.MOD_ID + ".config." + key + ".comment"));
            optionsPerCategory.computeIfAbsent(category, (k) -> new JsonArray()).add(option);
        }));

        final JsonArray categories = new JsonArray();
        for (final Map.Entry<String, JsonArray> entry : optionsPerCategory.entrySet())
        {
            final JsonObject category = new JsonObject();
            category.addProperty("key", entry.getKey());
            category.addProperty("name", LanguageHandler.translateKey(Constants.MOD_ID + ".config." + entry.getKey()));
            category.addProperty("description", LanguageHandler.translateKey(Constants.MOD_ID + ".config." + entry.getKey() + ".comment"));
            category.add("options", entry.getValue());
            categories.add(category);
        }

        final JsonObject root = new JsonObject();
        root.add("categories", categories);
        return root;
    }

    @FunctionalInterface
    private interface ConfigOptionConsumer
    {
        void accept(final String category, final String key, final String type, final JsonObject object);
    }

    @FunctionalInterface
    private interface ConfigOptionCategoryConsumer
    {
        void accept(final String key, final String type, final JsonObject object);
    }

    private record ConfigJsonBuilder(ConfigOptionConsumer optionAdder) implements IConfigBuilder
    {
        @Override
        public void createCategory(final String category, final Consumer<IConfigCategoryBuilder> categoryBuilder)
        {
            categoryBuilder.accept(new ConfigJsonCategoryBuilder((key, type, elem) -> optionAdder.accept(category, key, type, elem)));
        }

        private record ConfigJsonCategoryBuilder(ConfigOptionCategoryConsumer optionAdder) implements IConfigCategoryBuilder
        {
            @Override
            public ValueHolder<Integer> defineInteger(final String key, final int defaultValue, final int minValue, final int maxValue)
            {
                final JsonObject configOption = new JsonObject();
                configOption.addProperty("default", defaultValue);
                configOption.addProperty("min", minValue);
                configOption.addProperty("max", maxValue);
                optionAdder.accept(key, "integer", configOption);
                return null;
            }

            @Override
            public ValueHolder<Double> defineDouble(final String key, final double defaultValue, final double minValue, final double maxValue)
            {
                final JsonObject configOption = new JsonObject();
                configOption.addProperty("default", defaultValue);
                configOption.addProperty("min", minValue);
                configOption.addProperty("max", maxValue);
                optionAdder.accept(key, "double", configOption);
                return null;
            }

            @Override
            public ValueHolder<Long> defineLong(final String key, final long defaultValue, final long minValue, final long maxValue)
            {
                final JsonObject configOption = new JsonObject();
                configOption.addProperty("key", key);
                configOption.addProperty("type", "long");
                configOption.addProperty("default", defaultValue);
                configOption.addProperty("min", minValue);
                configOption.addProperty("max", maxValue);
                optionAdder.accept(key, "long", configOption);
                return null;
            }

            @Override
            public ValueHolder<Boolean> defineBoolean(final String key, final boolean defaultValue)
            {
                final JsonObject configOption = new JsonObject();
                configOption.addProperty("default", defaultValue);
                optionAdder.accept(key, "boolean", configOption);
                return null;
            }

            @Override
            public <V extends Enum<V>> ValueHolder<V> defineEnum(final String key, final V defaultValue)
            {
                final JsonObject configOption = new JsonObject();
                configOption.addProperty("default", defaultValue.name());

                final JsonArray enumValues = new JsonArray();
                for (final Enum value : defaultValue.getClass().getEnumConstants())
                {
                    enumValues.add(value.name());
                }
                configOption.add("values", enumValues);
                optionAdder.accept(key, "enum", configOption);
                return null;
            }

            @Override
            public <T> ValueHolder<List<? extends T>> defineList(final String key, final List<? extends T> defaultValue, final Predicate<Object> elementValidator)
            {
                final JsonObject configOption = new JsonObject();
                configOption.addProperty("default", defaultValue.stream().map(Object::toString).collect(Collectors.joining(", ")));
                optionAdder.accept(key, "list", configOption);
                return null;
            }
        }
    }
}
