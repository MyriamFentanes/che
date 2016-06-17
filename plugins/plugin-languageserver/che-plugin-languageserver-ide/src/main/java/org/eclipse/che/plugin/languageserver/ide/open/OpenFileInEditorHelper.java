/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.languageserver.ide.open;

import com.google.common.base.Strings;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.OpenEditorCallbackImpl;
import org.eclipse.che.ide.api.editor.text.TextRange;
import org.eclipse.che.ide.api.editor.texteditor.TextEditorPresenter;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.resource.Path;

/**
 * Util class, helps to open file by path in editor
 * @author Evgen Vidolob
 */
@Singleton
public class OpenFileInEditorHelper {

    private final EditorAgent              editorAgent;
    private final ProjectExplorerPresenter projectExplorer;

    @Inject
    public OpenFileInEditorHelper(EditorAgent editorAgent,
                                  ProjectExplorerPresenter projectExplorer) {
        this.editorAgent = editorAgent;
        this.projectExplorer = projectExplorer;
    }

    public void openFile(String filePath, TextRange selectionRange) {
        if (Strings.isNullOrEmpty(filePath)) {
            return;
        }

        EditorPartPresenter editorPartPresenter = editorAgent.getOpenedEditor(Path.valueOf(filePath));
        if (editorPartPresenter != null) {
            editorAgent.activateEditor(editorPartPresenter);
            fileOpened(editorPartPresenter, selectionRange);
            return;
        }

        projectExplorer.getNodeByPath(new HasStorablePath.StorablePath(filePath))
                       .then(selectNode())
                       .then(openNode(selectionRange));
    }

    public void openFile(String filePath) {
        openFile(filePath, null);
    }

    private Function<Node, Node> openNode(final TextRange selectionRange) {
        return new Function<Node, Node>() {
            @Override
            public Node apply(Node node) {
                if (node instanceof FileReferenceNode) {
                    openFile((VirtualFile)node, selectionRange);
                }
                return node;
            }
        };
    }

    private Function<Node, Node> selectNode() {
        return new Function<Node, Node>() {
            @Override
            public Node apply(Node node){
                projectExplorer.select(node, false);
                return node;
            }
        };
    }

    private void openFile(VirtualFile result, final TextRange selectionRange) {
        editorAgent.openEditor(result, new OpenEditorCallbackImpl() {
            @Override
            public void onEditorOpened(EditorPartPresenter editor) {
                fileOpened(editor, selectionRange);
            }
        });
    }

    private void fileOpened(final EditorPartPresenter editor, final TextRange selectionRange) {
        if (editor instanceof TextEditorPresenter && selectionRange != null) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    ((TextEditorPresenter)editor).getDocument().setSelectedRange(selectionRange, true);
                }
            });
        }
    }
}
