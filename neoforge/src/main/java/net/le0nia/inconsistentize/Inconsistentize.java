package net.le0nia.inconsistentize;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class Inconsistentize {

    public Inconsistentize(IEventBus eventBus) {
        CommonClass.init();
    }
}
