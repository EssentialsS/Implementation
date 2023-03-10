package org.essentialss.implementation.messages.adapter.player.command;

import net.kyori.adventure.text.Component;
import org.essentialss.api.config.value.SingleConfigValue;
import org.essentialss.api.message.MessageManager;
import org.essentialss.api.message.adapters.player.command.WhoIsMessageAdapter;
import org.essentialss.api.message.placeholder.SPlaceHolder;
import org.essentialss.api.player.data.SGeneralUnloadedData;
import org.essentialss.implementation.EssentialsSMain;
import org.essentialss.implementation.config.value.modifiers.SingleDefaultConfigValueWrapper;
import org.essentialss.implementation.config.value.simple.ComponentConfigValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collection;

public class WhoIsCommandManagerAdapterImpl implements WhoIsMessageAdapter {

    private static final SingleConfigValue.Default<Component> CONFIG_VALUE = new SingleDefaultConfigValueWrapper<>(
            new ComponentConfigValue("player", "command", "WhoIsCommand"), Component.text("%% is %%"));

    private @Nullable Component component;

    public WhoIsCommandManagerAdapterImpl() {
        try {
            this.component = CONFIG_VALUE.parse(EssentialsSMain.plugin().messageManager().get().config().get());
        } catch (SerializationException e) {
            this.component = null;
        }
    }

    @Override
    public Component adaptMessage(@NotNull Component messageToAdapt, @NotNull SGeneralUnloadedData player) {
        MessageManager messageManager = EssentialsSMain.plugin().messageManager().get();
        for (SPlaceHolder<SGeneralUnloadedData> holder : messageManager.mappedPlaceholdersFor(SGeneralUnloadedData.class)) {
            if (holder.canApply(messageToAdapt)) {
                messageToAdapt = holder.apply(messageToAdapt, player);
            }
        }
        return messageToAdapt;
    }

    @Override
    public @NotNull SingleConfigValue.Default<Component> configValue() {
        return CONFIG_VALUE;
    }

    @Override
    public @NotNull Collection<SPlaceHolder<?>> supportedPlaceholders() {
        MessageManager messageManager = EssentialsSMain.plugin().messageManager().get();
        return messageManager.placeholdersFor(SGeneralUnloadedData.class);
    }

    @Override
    public @NotNull Component unadaptedMessage() {
        if (null == this.component) {
            return this.defaultUnadaptedMessage();
        }
        return this.component;
    }
}
