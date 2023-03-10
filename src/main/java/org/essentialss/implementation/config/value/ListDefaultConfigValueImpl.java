package org.essentialss.implementation.config.value;

import org.essentialss.api.config.value.CollectionConfigValue;
import org.essentialss.api.config.value.ConfigValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.LinkedList;
import java.util.List;

public class ListDefaultConfigValueImpl<T> implements CollectionConfigValue<T> {

    private final @NotNull Object[] nodes;
    private final ConfigValue<T> parse;

    public ListDefaultConfigValueImpl(@NotNull ConfigValue<T> parse, @NotNull Object... nodes) {
        this.nodes = nodes;
        this.parse = parse;
    }

    @Override
    public @NotNull Object[] nodes() {
        return this.nodes;
    }

    @SuppressWarnings("allow-nullable")
    @Override
    public @Nullable List<T> parse(@NotNull ConfigurationNode root) throws SerializationException {
        ConfigurationNode node = root.node(this.nodes());
        List<T> list = new LinkedList<>();
        for (ConfigurationNode n : node.childrenList()) {
            list.add(this.parse.parse(n));
        }
        return list;
    }

    @Override
    public void set(@NotNull ConfigurationNode root, @Nullable List<T> value) throws SerializationException {
        ConfigurationNode node = root.node(this.nodes());
        if (null == value) {
            node.set(null);
            return;
        }

        ConfigurationNode listNode = node.appendListNode();
        for (T i : value) {
            this.parse.set(listNode, i);
            listNode = listNode.appendListNode();
        }
    }
}
