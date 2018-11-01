package org.workcraft.plugins.xmas.tools;

import org.workcraft.commands.Command;
import org.workcraft.dom.Node;
import org.workcraft.gui.graph.GraphEditorPanel;
import org.workcraft.gui.graph.tools.AbstractGraphEditorTool;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.interop.ExternalProcess;
import org.workcraft.plugins.xmas.VisualXmas;
import org.workcraft.plugins.xmas.Xmas;
import org.workcraft.plugins.xmas.XmasSettings;
import org.workcraft.plugins.xmas.components.QueueComponent;
import org.workcraft.plugins.xmas.components.SyncComponent;
import org.workcraft.plugins.xmas.components.VisualQueueComponent;
import org.workcraft.plugins.xmas.components.VisualSyncComponent;
import org.workcraft.plugins.xmas.gui.SolutionsDialog1;
import org.workcraft.plugins.xmas.gui.SolutionsDialog2;
import org.workcraft.util.DialogUtils;
import org.workcraft.util.FileUtils;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class XmasAnalysisTool extends AbstractGraphEditorTool implements Command {

    private static class Qslist {
        String name;
        int chk;

        Qslist(String s1, int n) {
            name = s1;
            chk = n;
        }
    }

    int cntSyncNodes = 0;
    int index = 1;
    JFrame mainFrame = null;
    static String level = "";
    static String display = "";
    static String highlight = "";
    static String soln = "";
    static List<Qslist> qslist = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return "Analysis";
    }

    @Override
    public String getSection() {
        return "Verification";
    }

    public void dispose() {
        mainFrame.setVisible(false);
    }

    public List<JRadioButton> rlist = new ArrayList<>();

    private static List<String> processArg(String file, int index) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(file));
        } catch (FileNotFoundException e) {
            LogUtils.logError(e.getMessage());
        }
        String targ = "";
        String larg = "";
        String sarg = "";
        String aarg = "";
        while (sc.hasNextLine()) {
            Scanner line = new Scanner(sc.nextLine());
            Scanner nxt = new Scanner(line.next());
            String check = nxt.next();
            String str;
            if (check.startsWith("trace")) {
                nxt = new Scanner(line.next());
                targ = "-t";
                targ = targ + nxt.next();
            } else if (check.startsWith("level")) {
                nxt = new Scanner(line.next());
                larg = "-v";
                str = nxt.next();
                level = str;
                if (str.equals("normal")) {
                    //System.out.println("Read v1");
                    larg = "-v1";
                } else if (str.equals("advanced")) {
                    //System.out.println("Read v2");
                    larg = "-v2";
                }
            } else if (check.startsWith("display")) {
                nxt = new Scanner(line.next());
                str = nxt.next();
                //System.out.println("strrr=" + str);
                display = str;
            } else if (check.startsWith("highlight")) {
                nxt = new Scanner(line.next());
                str = nxt.next();
                //System.out.println("strrr=" + str);
                highlight = str;
            } else if (check.startsWith("soln")) {
                nxt = new Scanner(line.next());
                str = nxt.next();
                //System.out.println("solnnnnnnnnnnnnnnnnn=" + str);
                soln = str;
                sarg = "-s" + str;
            }
        }
        //System.out.println("aaaaaaaaaaaindex==============" + index);
        aarg = "-a" + index;
        //System.out.println("aaaaaaaaaaaaaaarggggg=" + aarg);
        ArrayList<String> args = new ArrayList<>();
        if (!targ.isEmpty()) args.add(targ);
        if (!larg.isEmpty()) args.add(larg);
        if (!sarg.isEmpty()) args.add(sarg);
        if (!aarg.isEmpty()) args.add(aarg);
        return args;
    }

    private static String processLoc(String file) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(file));
        } catch (FileNotFoundException e) {
            LogUtils.logError(e.getMessage());
        }
        String str = "";
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            //System.out.println(sc.next());
            str = str + line + '\n';
        }
        return str;
    }

    private static void processQsl(String file) {
        qslist.clear();
        Scanner sc = null;
        try {
            sc = new Scanner(new File(file));
        } catch (FileNotFoundException e) {
            LogUtils.logError(e.getMessage());
        }
        while (sc.hasNextLine()) {
            Scanner line = new Scanner(sc.nextLine());
            Scanner nxt = new Scanner(line.next());
            String check = nxt.next();
            nxt = new Scanner(line.next());
            String str = nxt.next();
            int num = Integer.parseInt(str);
            //System.out.println("qsl " + check + " " + str + " " + num);
            qslist.add(new Qslist(check, num));
        }
    }

    private static String processEq(String file) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(file));
        } catch (FileNotFoundException e) {
            LogUtils.logError(e.getMessage());
        }
        String str = "";
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            //System.out.println(sc.next());
            str = str + line + '\n';
        }
        return str;
    }

    public int checkType(String s) {

        if (s.contains("DEADLOCK FREE")) {
            return 0;
        } else if (s.contains("TRACE FOUND")) {
            return 1;
        } else if (s.contains("Local")) {
            return 2;
        }
        return -1;
    }

    public void initHighlight(Xmas xnet, VisualXmas vnet) {
        VisualQueueComponent vqc;
        VisualSyncComponent vsc;

        for (Node node : vnet.getNodes()) {
            if (node instanceof VisualQueueComponent) {
                vqc = (VisualQueueComponent) node;
                vqc.setForegroundColor(Color.black);
            } else if (node instanceof VisualSyncComponent) {
                vsc = (VisualSyncComponent) node;
                vsc.setForegroundColor(Color.black);
            }
        }
    }

    public void localHighlight(String s, Xmas xnet, VisualXmas vnet) {
        QueueComponent qc;
        SyncComponent sc;
        VisualQueueComponent vqc;
        VisualSyncComponent vsc;

        //System.out.println("s=" + s);
        for (String st : s.split(" |\n")) {
            if (st.startsWith("Q") || st.startsWith("S")) {
                System.out.println(st);
                for (Node node : vnet.getNodes()) {
                    if (node instanceof VisualQueueComponent) {
                        vqc = (VisualQueueComponent) node;
                        qc = vqc.getReferencedQueueComponent();
                        //if (xnet.getName(qc).contains(st)) {
                        String rstr;
                        rstr = xnet.getName(qc);
                        rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                        if (rstr.equals(st)) {
                            vqc.setForegroundColor(Color.red);
                        }
                    } else if (node instanceof VisualSyncComponent) {
                        vsc = (VisualSyncComponent) node;
                        sc = vsc.getReferencedSyncComponent();
                        //if (xnet.getName(qc).contains(st)) {
                        String rstr;
                        rstr = xnet.getName(sc);
                        rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                        if (rstr.equals(st)) {
                            vsc.setForegroundColor(Color.red);
                        }
                    }
                }
            }
        }
    }

    public void relHighlight(String s, Xmas xnet, VisualXmas vnet) {
        int typ = 0;
        String str = "";
        QueueComponent qc;
        SyncComponent sc;
        VisualQueueComponent vqc;
        VisualSyncComponent vsc;

        for (String st : s.split(" |;|\n")) {
            if (st.contains("->")) {
                //System.out.println("testst" + st);
                typ = 0;
                for (String st2 : st.split("->")) {
                    str = st2;
                    //System.out.println("str===" + str);
                    for (Node node : vnet.getNodes()) {
                        if (node instanceof VisualQueueComponent) {
                            vqc = (VisualQueueComponent) node;
                            qc = vqc.getReferencedQueueComponent();
                            //System.out.println("x===" + xnet.getName(qc));
                            String rstr;
                            rstr = xnet.getName(qc);
                            rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                            if (rstr.equals(str) && typ == 0) {
                                vqc.setForegroundColor(Color.pink);
                            }
                        } else if (node instanceof VisualSyncComponent) {
                            vsc = (VisualSyncComponent) node;
                            sc = vsc.getReferencedSyncComponent();
                            //System.out.println("strrr===" + str + ' ' + xnet.getName(sc));
                            String rstr;
                            rstr = xnet.getName(sc);
                            rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                            if (rstr.equals(str) && typ == 0) {
                                vsc.setForegroundColor(Color.pink);
                            }
                        }
                    }
                }
            } else if (st.contains("<-")) {
                //System.out.println("testst_" + st);
                typ = 1;
                for (String st2 : st.split("<-")) {
                    str = st2;
                    //System.out.println("str===" + str);
                    for (Node node : vnet.getNodes()) {
                        if (node instanceof VisualQueueComponent) {
                            vqc = (VisualQueueComponent) node;
                            qc = vqc.getReferencedQueueComponent();
                            String rstr;
                            rstr = xnet.getName(qc);
                            rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                            if (rstr.equals(str) && typ == 1) {
                                vqc.setForegroundColor(Color.red);
                            }
                        } else if (node instanceof VisualSyncComponent) {
                            vsc = (VisualSyncComponent) node;
                            sc = vsc.getReferencedSyncComponent();
                            String rstr;
                            rstr = xnet.getName(sc);
                            rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                            if (rstr.equals(str) && typ == 1) {
                                vsc.setForegroundColor(Color.red);
                            }
                        }
                    }
                }
            }
        }
    }

    public void activeHighlight(Xmas xnet, VisualXmas vnet) {
        QueueComponent qc;
        SyncComponent sc;
        VisualQueueComponent vqc;
        VisualSyncComponent vsc;

        for (Qslist ql : qslist) {
            if (ql.chk == 0) {
                for (Node node : vnet.getNodes()) {
                    if (node instanceof VisualQueueComponent) {
                        vqc = (VisualQueueComponent) node;
                        qc = vqc.getReferencedQueueComponent();
                        String rstr;
                        rstr = xnet.getName(qc);
                        rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                        if (rstr.equals(ql.name)) {
                            vqc.setForegroundColor(Color.green);
                        }
                    } else if (node instanceof VisualSyncComponent) {
                        vsc = (VisualSyncComponent) node;
                        sc = vsc.getReferencedSyncComponent();
                        String rstr;
                        rstr = xnet.getName(sc);
                        rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                        if (rstr.equals(ql.name)) {
                            vsc.setForegroundColor(Color.green);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Xmas.class);
    }

    GraphEditorPanel editor1;
    Graphics2D g;

    static List<JCheckBox> jcbn = new ArrayList<>();
    JCheckBox jcb, jcblast;

    void createPanel(List<JPanel> panellist, String file) {
        int no = 1;
        Scanner sc = null;
        try {
            sc = new Scanner(new File(file));
        } catch (FileNotFoundException e) {
            LogUtils.logError(e.getMessage());
        }
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            Scanner lineSc = new Scanner(line);
            if (line.contains("SOLUTION")) {
                if (no > 1) {
                    panellist.get(panellist.size() - 1).add(jcb = new JCheckBox(""));
                    ItemListener itemListener = new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            if (e.getSource() instanceof JCheckBox) {
                                JCheckBox sjcb = (JCheckBox) e.getSource();
                                if (sjcb.isSelected()) index = jcbn.indexOf(sjcb) + 1;
                                if (sjcb.isSelected()) System.out.println("indexa==" + index);
                                if (jcblast != null) jcblast.setSelected(false);
                                jcblast = sjcb;
                                //String name = sjcb.getName();
                                //System.out.println(name);
                            }
                        }
                    };
                    jcb.addItemListener(itemListener);
                    jcbn.add(jcb);
                }
                panellist.add(new JPanel());
                panellist.get(panellist.size() - 1).add(new JLabel(" Soln" + no + ": "));
                no++;
            } else if (line.contains("Qu")) {
                Scanner nxt = new Scanner(lineSc.next());
                String check = nxt.next();
                nxt = new Scanner(lineSc.next());
                panellist.get(panellist.size() - 1).add(new JLabel(check));
                panellist.get(panellist.size() - 1).add(new JTextField(nxt.next(), 1));
            }
            lineSc.close();
        }
        panellist.get(panellist.size() - 1).add(jcb = new JCheckBox(""));

        ItemListener itemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() instanceof JCheckBox) {
                    JCheckBox sjcb = (JCheckBox) e.getSource();
                    if (sjcb.isSelected()) index = jcbn.indexOf(sjcb) + 1;
                    //if (sjcb.isSelected()) System.out.println("indexb==" + index);
                    if (jcblast != null) jcblast.setSelected(false);
                    jcblast = sjcb;
                    //String name = sjcb.getName();
                    //System.out.println(name);
                }
            }
        };
        jcb.addItemListener(itemListener);
        jcbn.add(jcb);
    }

    @Override
    public void run(WorkspaceEntry we) {
        System.out.println("Analysing Model");

        final VisualXmas vnet = WorkspaceUtils.getAs(we, VisualXmas.class);
        final Xmas xnet = WorkspaceUtils.getAs(we, Xmas.class);

        mainFrame = new JFrame("Analysis");
        JPanel panelmain = new JPanel();
        mainFrame.getContentPane().add(panelmain, BorderLayout.PAGE_START);
        panelmain.setLayout(new BoxLayout(panelmain, BoxLayout.PAGE_AXIS));
        List<JPanel> panellist = new ArrayList<>();

        JPanel panela = new JPanel();
        panela.setLayout(new FlowLayout(FlowLayout.LEFT));
        panela.add(new JLabel(" UNIQUE SOLUTIONS "));
        panela.add(Box.createHorizontalGlue());
        panelmain.add(panela);

        jcbn.clear();
        File solnFile = XmasSettings.getTempVxmSolnFile();
        createPanel(panellist, solnFile.getAbsolutePath());
        for (JPanel plist : panellist) {
            panelmain.add(plist);
        }

        JPanel panelb = new JPanel();
        panelb.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton okButton = new JButton("OK");
        panelb.add(Box.createHorizontalGlue());
        panelb.add(cancelButton);
        panelb.add(okButton);
        panelmain.add(panelb);

        mainFrame.pack();
        mainFrame.setVisible(true);

        cancelButton.addActionListener(event -> dispose());

        okButton.addActionListener(event -> {
            dispose();
            if (index != 0) {
                try {
                    File cpnFile = XmasSettings.getTempVxmCpnFile();
                    File inFile = XmasSettings.getTempVxmInFile();
                    FileUtils.copyFile(cpnFile, inFile);

                    ArrayList<String> vxmCommand = new ArrayList<>();
                    vxmCommand.add(XmasSettings.getTempVxmCommandFile().getAbsolutePath());
                    vxmCommand.addAll(processArg(XmasSettings.getTempVxmVsettingsFile().getAbsolutePath(), index));
                    ExternalProcess.printCommandLine(vxmCommand);
                    String[] cmdArray = vxmCommand.toArray(new String[vxmCommand.size()]);
                    Process vxmProcess = Runtime.getRuntime().exec(cmdArray, null, XmasSettings.getTempVxmDirectory());

                    String s, str = "";
                    InputStreamReader inputStreamReader = new InputStreamReader(vxmProcess.getInputStream());
                    BufferedReader stdInput = new BufferedReader(inputStreamReader);
                    int n = 0;
                    int test = -1;
                    initHighlight(xnet, vnet);
                    while ((s = stdInput.readLine()) != null) {
                        //if (n == 1) test = checkType(s);
                        if (test == -1) test = checkType(s);
                        if (n > 0) str = str + s + '\n';
                        n++;
                        System.out.println(s);
                    }
                    if (level.equals("advanced")) {
                        System.out.println("LEVEL IS ADVANCED ");
                        File qslFile = XmasSettings.getTempVxmQslFile();
                        processQsl(qslFile.getAbsolutePath());

                        File equFile = XmasSettings.getTempVxmEquFile();
                        str = processEq(equFile.getAbsolutePath()); //testing str assignment - fpb
                    } else if (level.equals("normal") && (test == 2)) {
                        System.out.println("LEVEL IS NORMAL ");
                        File locFile = XmasSettings.getTempVxmLocFile();
                        str = processLoc(locFile.getAbsolutePath());
                    }
                    if (test > 0) {
                        if (display.equals("popup")) {
                            if (!level.equals("advanced")) {
                                new SolutionsDialog1(test, str);
                            } else {
                                new SolutionsDialog2(test, str);
                            }
                        }
                        if (test == 2) {
                            if (highlight.equals("local")) {
                                localHighlight(str, xnet, vnet);
                            } else if (highlight.equals("rel")) {
                                relHighlight(str, xnet, vnet);
                                activeHighlight(xnet, vnet);
                            }
                        }
                    } else if (test == 0) {
                        if (display.equals("popup")) {
                            DialogUtils.showInfo("The system is deadlock-free.");
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public Decorator getDecorator(GraphEditor editor) {
        return null;
    }

}
