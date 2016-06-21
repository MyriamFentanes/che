/**
 * Copyright (c) 2016 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.che.plugin.languageserver.shared.lsapi;

import io.typefox.lsapi.WorkspaceSymbolParams;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface WorkspaceSymbolParamsDTO extends WorkspaceSymbolParams {
    /**
     * A non-empty query string
     * 
     */
    public abstract void setQuery(final String query);

    //TODO this is temporary, until we don't have binding LS to project not a file
    String getFileUri();

    void setFileUri(String fileUri);
}