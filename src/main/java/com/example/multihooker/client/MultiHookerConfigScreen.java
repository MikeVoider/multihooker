package com.example.multihooker.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;

/**
 * MultiHooker's in-game config screen.
 *
 * <p>Two things are deliberately kept out of reach:
 * <ul>
 *   <li>the whole "Rendering" and "Diagnostics" sections - these are internal
 *       tuning options, so the section buttons are hidden and players cannot
 *       change them from the UI;</li>
 *   <li>the SeaMaster Relic's own catch chance, which is filtered out of the
 *       "Fishing" section (see {@link #isHiddenEntry}).</li>
 * </ul>
 *
 * <p>Everything else (crate chances, AquaKey rate, points, ...) stays editable.
 * The values themselves are still in the config file for anyone who really
 * wants to edit them by hand.
 */
public final class MultiHookerConfigScreen extends ConfigurationSectionScreen {
   public MultiHookerConfigScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title, Filter filter) {
      super(parent, type, modConfig, title, filter);
   }

   @Override
   protected Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
      return this.isHiddenSection(key) ? null : super.createSection(key, subconfig, subsection);
   }

   @Override
   protected Element createOtherSection(String key, Object rawValue) {
      return this.isHiddenSection(key) ? null : super.createOtherSection(key, rawValue);
   }

   /** Only the top level hides sections - a nested key of the same name is left alone. */
   private boolean isHiddenSection(String key) {
      return this.context.keylist().isEmpty() && isLockedSection(key);
   }

   /** Internal sections that must not be editable from the config UI. */
   public static boolean isLockedSection(String key) {
      return "rendering".equals(key) || "diagnostics".equals(key);
   }

   /** Values that must not appear in the config UI (the relic keeps its own rate). */
   public static boolean isHiddenEntry(String key) {
      return key != null && key.endsWith("seaMasterDropChance");
   }
}
