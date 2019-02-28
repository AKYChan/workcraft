package org.workcraft.plugins.fst.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.workcraft.Framework;
import org.workcraft.commands.MenuOrdering;
import org.workcraft.commands.Command;
import org.workcraft.utils.DesktopApi;
import org.workcraft.plugins.fst.Fst;
import org.workcraft.plugins.fst.ProcessWindowsSettings;
import org.workcraft.plugins.fst.VisualFst;
import org.workcraft.plugins.fst.interop.SgExporter;
import org.workcraft.plugins.fst.tasks.LtscatResultHandler;
import org.workcraft.plugins.fst.tasks.LtscatTask;
import org.workcraft.tasks.TaskManager;
import org.workcraft.utils.FileUtils;
import org.workcraft.utils.ExecutableUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.utils.WorkspaceUtils;

public class ExtractWindowsCommand implements Command, MenuOrdering {

    @Override
    public final String getSection() {
        return "!    Conversion"; // 4 spaces - positions 1st
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public String getDisplayName() {
        return "Extract windows";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicableExact(we, VisualFst.class);
    }

    @Override
    public void run(WorkspaceEntry we) {
        File dir;
        File sgFile;
        File scriptFile;
        String sgFileName = we.getTitle() + ProcessWindowsSettings.getExportedFstExtension();
        String scriptFileName = ProcessWindowsSettings.getScriptName();
        SgExporter exporter = new SgExporter();

        // temporary directory
        String prefix = FileUtils.getTempPrefix(we.getTitle());
        dir = FileUtils.createTempDirectory(prefix);

        // get model name
        Fst fst = WorkspaceUtils.getAs(we, Fst.class);
        Framework framework = Framework.getInstance();

        sgFile = new File(dir, sgFileName);

        // exporting the file
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(sgFile);
            exporter.export(fst, fos);
        } catch (Exception e) {
            e.printStackTrace();
            FileUtils.deleteOnExitRecursively(dir);
        }
        // writing the script for ltscat
        scriptFile = writeScript(dir, scriptFileName, sgFile.getAbsolutePath(), we.getTitle());

        // calling ltscat
        final LtscatTask ltscatTask = new LtscatTask(we, dir, scriptFile);
        final LtscatResultHandler ltscatResult = new LtscatResultHandler(ltscatTask, dir);
        final TaskManager taskManager = framework.getTaskManager();
        taskManager.queue(ltscatTask, "Ltscat - process windows", ltscatResult);

    }

    private File writeScript(File dir, String scriptName, String sgName, String title) {
        File script = new File(dir, scriptName);
        File ltscatPath = new File(ExecutableUtils.getAbsoluteCommandPath(ProcessWindowsSettings.getLtscatFolder()));
        String ltscatModule = ProcessWindowsSettings.getLtscatModuleName();

        try {
            PrintWriter writer = new PrintWriter(script);
            writer.println("import sys");
            writer.println("sys.path.append('" + ltscatPath.getAbsolutePath() + "')");
            writer.println("from " + ltscatModule + " import *");
            writer.println("from " + ltscatModule + " import LtsCat as lts");
            writer.println("from " + ltscatModule + " import LtsCat_Windows as win");
            writer.println("l = lts('" + sgName + "')");
            writer.println("l.extractWindows(prefix='"
                    + dir.getAbsolutePath()
                    + (DesktopApi.getOs().isWindows() ? "\\" : "/")
                    + title
                    + "')");
            writer.println("exit");
            writer.close();
        } catch (IOException e) {
            FileUtils.deleteOnExitRecursively(dir);
            e.printStackTrace();
        }

        return script;

    }

}
