package dev.rinchan.killconfirmed.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.rinchan.killconfirmed.client.KillConfirmedConfigScreen;

public final class KillConfirmedModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KillConfirmedConfigScreen::new;
    }
}
