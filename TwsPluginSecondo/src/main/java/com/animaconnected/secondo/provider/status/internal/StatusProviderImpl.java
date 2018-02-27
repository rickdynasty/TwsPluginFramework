package com.animaconnected.secondo.provider.status.internal;

import com.animaconnected.secondo.provider.status.StatusChangeListener;
import com.animaconnected.secondo.provider.status.StatusController;
import com.animaconnected.secondo.provider.status.StatusModel;
import com.animaconnected.secondo.provider.status.StatusProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class StatusProviderImpl implements StatusProvider {

    private final List<StatusController> mControllers = new ArrayList<StatusController>();
    private final Set<StatusChangeListener> mListeners = new CopyOnWriteArraySet<StatusChangeListener>();
    private StatusModel mStatus = null;

    public void addController(final StatusController controller) {
        mControllers.add(controller);
        update();
    }

    @Override
    public void registerListener(final StatusChangeListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterListener(final StatusChangeListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public StatusModel getCurrent() {
        return mStatus;
    }

    @Override
    public void onStatusChanged() {
        update();
    }

    private void update() {
        final StatusModel status = calculateHighestPrioStatus();
        if (!isSameType(status, mStatus)) {
            mStatus = status;
            notifyChanged();
        }
    }

    private void notifyChanged() {
        for (StatusChangeListener listener : mListeners) {
            listener.onStatusChanged();
        }
    }

    private boolean isSameType(final StatusModel lhs, final StatusModel rhs) {
        if (lhs == null && rhs == null) {
            return true;
        } else if (lhs != null && rhs != null) {
            return lhs.isSameType(rhs);
        } else {
            return false;
        }
    }

    private StatusModel calculateHighestPrioStatus() {
        final List<StatusModel> statusList = new ArrayList<StatusModel>();

        for (StatusController controller : mControllers) {
            final StatusModel status = controller.getCurrent();
            if (status != null) {
                statusList.add(status);
            }
        }

        Collections.sort(statusList, new Comparator<StatusModel>() {
            @Override
            public int compare(final StatusModel lhs, final StatusModel rhs) {
                return Integer.compare(rhs.getPriority(), lhs.getPriority());
            }
        });

        return statusList.isEmpty() ? null : statusList.get(0);
    }
}
