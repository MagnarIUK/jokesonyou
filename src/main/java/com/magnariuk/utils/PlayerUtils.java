package com.magnariuk.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PlayerUtils {


        public static void sendMessage(ServerPlayer player, Component component, boolean overlay) {
            player.sendSystemMessage(
                component,
                true
            );
        }
        public static void sendMessage(ServerPlayer player, Component component) {
            sendMessage(player, component, true);
        }
}