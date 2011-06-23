package com.jetbrains.pyqt;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.django.run.Runner;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yole
 */
public class CompileQrcAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(PlatformDataKeys.PROJECT);
    VirtualFile[] vFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    assert vFiles != null;
    Module module = e.getData(LangDataKeys.MODULE);
    String path = QtFileType.findQtTool(module, "pyrcc4");
    if (path == null) {
      path = QtFileType.findQtTool(module, "pyside-rcc");
    }
    if (path == null) {
      Messages.showErrorDialog(project, "Could not find pyrcc4 or pyside-rcc for selected Python interpreter", "Compile .qrc file");
      return;
    }
    CompileQrcDialog dialog = new CompileQrcDialog(project, vFiles);
    dialog.show();
    if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return;
    }
    List<String> args = new ArrayList<String>(Arrays.asList(path, "-o", dialog.getOutputPath()));
    for (VirtualFile vFile : vFiles) {
      args.add(vFile.getPath());
    }
    try {
      Runner.runInPath(project, vFiles[0].getParent().getPath(), true, null, args.toArray(new String[args.size()]));
    }
    catch (ExecutionException ex) {
      Messages.showErrorDialog(project, "Error running " + path + ": " + ex.getMessage(), "Compile .qrc file");
    }
  }

  @Override
  public void update(AnActionEvent e) {
    Module module = e.getData(LangDataKeys.MODULE);
    VirtualFile[] vFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    e.getPresentation().setVisible(module != null && filesAreQrc(vFiles));
  }

  private static boolean filesAreQrc(VirtualFile[] vFiles) {
    if (vFiles == null || vFiles.length == 0) {
      return false;
    }
    for (VirtualFile vFile : vFiles) {
      if (!"qrc".equals(FileUtil.getExtension(vFile.getName()))) {
        return false;
      }
    }
    return true;
  }

  public static class CompileQrcDialog extends DialogWrapper {
    private JPanel myPanel;
    private TextFieldWithBrowseButton myOutputFileField;

    protected CompileQrcDialog(Project project, VirtualFile[] vFiles) {
      super(project);
      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      return myPanel;
    }

    public String getOutputPath() {
      return myOutputFileField.getText();
    }
  }
}
