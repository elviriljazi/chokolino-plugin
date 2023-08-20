package org.intellij.sdk.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class PopulateGenerator extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        GeneratorWorker.getInstance().generate(e, false, true);
    }


}
