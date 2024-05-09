/*
 * Copyright (c) 2024 Logan Bateman.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */

package de.neemann.digital.core.io.zenoh;

/**
 * Interface for classes which send data using Zenoh
 */
public interface ZenohDataSender {
    public boolean publishingEnabled();
    public boolean isDataChanged();
    public void sendData();
}