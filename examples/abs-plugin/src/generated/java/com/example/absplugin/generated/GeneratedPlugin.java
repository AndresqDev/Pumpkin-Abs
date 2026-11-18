package com.example.absplugin.generated;

public final class GeneratedPlugin extends com.example.absplugin.AbsExamplePlugin {
    @Override
    protected void onEnable() {
        super.onEnable();
        GeneratedListeners.register(this);
    }
}
