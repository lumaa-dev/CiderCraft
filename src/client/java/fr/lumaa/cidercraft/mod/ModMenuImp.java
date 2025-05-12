package fr.lumaa.cidercraft.mod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fr.lumaa.cidercraft.CiderCraftClient;

public class ModMenuImp implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> CiderCraftClient.config.createSettings(parent);
    }
}
