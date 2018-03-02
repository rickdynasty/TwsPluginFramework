package com.example.pluginbluetooth.behaviour;

public interface Behaviour {

    boolean isExecuting();

    void execute(int action);

    int getDeviceComplicationMode();

    void activate(int slot);

    void deactivated(int slot);

    int getWeight();

    int[] compatibleSlots();

    String getType();

    String getSettingsAsJson();

    /**
     * Name used to identify the behaviour in the UI, translatable.
     *
     * @return Behaviour display name.
     */
    String getHumanReadableName();

    /**
     * Name used to identify the behaviour in Analytics, not translatable.
     *
     * @return Behaviour identifier name.
     */
    String getName();

    String getDescription();

    int getIconResourceId();

    String[] getRequiredPermissions();

    int getConfigurationDescription();

    boolean isConfigured();

    void startRefreshing();

    void stopRefreshing();

    void registerChangeListener(BehaviourChangeListener listener);

    void unregisterChangeListener(BehaviourChangeListener listener);

    boolean isSelectable();
}
