package org.eclipse.che.plugin.languageserver.ide.editor.codeassist;

import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.editor.codeassist.Completion;
import org.eclipse.che.ide.api.editor.codeassist.CompletionProposal;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.text.LinearRange;
import org.eclipse.che.ide.api.editor.text.TextPosition;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.plugin.languageserver.ide.LanguageServerResources;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;
import org.eclipse.che.plugin.languageserver.shared.lsapi.CompletionItemDTO;
import org.eclipse.che.plugin.languageserver.shared.lsapi.RangeDTO;
import org.eclipse.che.plugin.languageserver.shared.lsapi.TextDocumentIdentifierDTO;

class CompletionItemBasedCompletionProposal implements CompletionProposal {

    private final TextDocumentServiceClient documentServiceClient;
    private final TextDocumentIdentifierDTO documentId;
    private final LanguageServerResources   resources;
    private final Icon                      icon;
    private       CompletionItemDTO         completionItem;

    CompletionItemBasedCompletionProposal(CompletionItemDTO completionItem,
                                          TextDocumentServiceClient documentServiceClient,
                                          TextDocumentIdentifierDTO documentId,
                                          LanguageServerResources resources, Icon icon) {
        this.completionItem = completionItem;
        this.documentServiceClient = documentServiceClient;
        this.documentId = documentId;
        this.resources = resources;
        this.icon = icon;
    }

    @Override
    public Widget getAdditionalProposalInfo() {
        if (completionItem.getDocumentation() != null && !completionItem.getDocumentation().isEmpty()) {
            Label label = new Label(completionItem.getDocumentation());
            label.setWordWrap(true);
            label.getElement().getStyle().setFontSize(13, Style.Unit.PX);
            label.setSize("100%", "100%");
            return label;
        }
        return null;
    }

    @Override
    public String getDisplayString() {
        if (completionItem.getDetail() != null) {
            SafeHtmlBuilder builder = new SafeHtmlBuilder();
            builder.appendEscaped(completionItem.getLabel());
            builder.appendHtmlConstant(" <span class=\"" + resources.css().codeassistantDetail() + "\">");
            builder.appendEscaped(completionItem.getDetail());
            builder.appendHtmlConstant("</span>");
        }
        return completionItem.getLabel();
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public void getCompletion(final CompletionCallback callback) {
        //call resolve only if we dont have TextEdit in CompletionItem
        //TODO we need to  check also CompletionItem#getInsertText();
        if (completionItem.getTextEdit() == null) {
            completionItem.setTextDocumentIdentifier(documentId);
            documentServiceClient.resolveCompletionItem(completionItem).then(new Operation<CompletionItemDTO>() {
                @Override
                public void apply(CompletionItemDTO arg) throws OperationException {
                    callback.onCompletion(new CompletionImpl(arg));
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(getClass(), arg);
                    //TODO we need to respect server capabilities, some LS don't supports resolve completion item
                    callback.onCompletion(new CompletionImpl(completionItem));
                }
            });
        } else {
            callback.onCompletion(new CompletionImpl(completionItem));
        }
    }

    private static class CompletionImpl implements Completion {

        private CompletionItemDTO completionItem;

        public CompletionImpl(CompletionItemDTO completionItem) {
            this.completionItem = completionItem;
        }

        @Override
        public void apply(Document document) {
            //TODO in general resolve completion item may not provide getTextEdit, need to add checks
            if (completionItem.getTextEdit() != null) {
                RangeDTO range = completionItem.getTextEdit().getRange();
                int startOffset = document.getIndexFromPosition(
                        new TextPosition(range.getStart().getLine(), range.getStart().getCharacter()));
                int endOffset = document
                        .getIndexFromPosition(new TextPosition(range.getEnd().getLine(), range.getEnd().getCharacter()));
                document.replace(startOffset, endOffset - startOffset, completionItem.getTextEdit().getNewText());
            } else {
                String insertText = completionItem.getInsertText() == null ? completionItem.getLabel() : completionItem.getInsertText();
                document.replace(document.getCursorOffset(), 0, insertText);
            }
        }

        @Override
        public LinearRange getSelection(Document document) {
            RangeDTO range = completionItem.getTextEdit().getRange();
            int startOffset = document
                                      .getIndexFromPosition(new TextPosition(range.getStart().getLine(), range.getStart().getCharacter()))
                              + completionItem.getTextEdit().getNewText().length();
            return LinearRange.createWithStart(startOffset).andLength(0);
        }

    }

}
