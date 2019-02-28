package org.workcraft.plugins.xmas.tools;

import org.workcraft.commands.Command;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.gui.editor.GraphEditorPanel;
import org.workcraft.gui.tools.AbstractGraphEditorTool;
import org.workcraft.gui.tools.Decorator;
import org.workcraft.gui.tools.GraphEditor;
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
import org.workcraft.utils.DialogUtils;
import org.workcraft.utils.FileUtils;
import org.workcraft.utils.Hierarchy;
import org.workcraft.utils.LogUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.utils.WorkspaceUtils;

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

public class XmasQueryTool extends AbstractGraphEditorTool implements Command {

    @Override
    public String getSection() {
        return "Verification";
    }

    @Override
    public String getDisplayName() {
        return "Query";
    }

    private static class Qslist {
        String name;
        int chk;

        Qslist(String s1, int n) {
            name = s1;
            chk = n;
        }
    }

    int cntSyncNodes = 0;
    int index = 0;
    static int q3flag = 0;
    JFrame mainFrame = null;
    JComboBox mdcombob = null;
    static JComboBox q1combob = null;
    static JComboBox q2combob = null;
    static JComboBox qscombob = null;
    static String level = "";
    static String display = "";
    static String highlight = "";
    static String soln = "";
    static List<Qslist> qslist = new ArrayList<>();

    public void dispose() {
        mainFrame.setVisible(false);
    }

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
        String qarg = "";
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
        //aarg = "-a" + index;
        if (index > 0) {
            String queue1 = "";
            String queue2 = "";
            String rstr1 = "";
            String rstr2 = "";
            q3flag = 0;
            if (index == 2) {
                queue1 = (String) q1combob.getSelectedItem();
                rstr1 = queue1;
                rstr1 = rstr1.replace(rstr1.charAt(0), Character.toUpperCase(rstr1.charAt(0)));
                queue2 = (String) q2combob.getSelectedItem();
                rstr2 = queue2;
                rstr2 = rstr2.replace(rstr2.charAt(0), Character.toUpperCase(rstr2.charAt(0)));
            } else if (index == 3) {
                q3flag = 1;
                queue1 = (String) qscombob.getSelectedItem();
                rstr1 = queue1;
                rstr1 = rstr1.replace(rstr1.charAt(0), Character.toUpperCase(rstr1.charAt(0)));
            }
            qarg = "-q" + index + rstr1 + rstr2;
        }
        //System.out.println("aaaaaaaaaaaaaaarggggg=" + aarg);
        ArrayList<String> args = new ArrayList<>();
        if (!targ.isEmpty()) args.add(targ);
        if (!larg.isEmpty()) args.add(larg);
        if (!sarg.isEmpty()) args.add(sarg);
        if (!aarg.isEmpty()) args.add(aarg);
        if (!qarg.isEmpty()) args.add(qarg);
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

    private static String processQue(String file) {
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
            //if (st.startsWith("Q")) {
            if (st.contains("->")) {
                //System.out.println("testst" + st);
                typ = 0;
                for (String st2 : st.split("->")) {
                    str = st2;
                    // System.out.println("str===" + str);
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

            //}
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

    void populateMd(int grnum) {
        int i;

        mdcombob.addItem("ALL");
        for (i = 1; i <= grnum; i++) {
            int n = i;
            mdcombob.addItem("L" + n);
        }
    }

    void populateQlists(Xmas cnet) {
        for (Node node : cnet.getNodes()) {
            if (node instanceof QueueComponent) {
                //System.out.println("QQQQ " + cnet.getName(node) + ".");
                q1combob.addItem(cnet.getName(node));
                q2combob.addItem(cnet.getName(node));
            }
        }
    }

    void populateQslists(Xmas cnet) {
        int cnt = 0;

        for (Node node : cnet.getNodes()) {
            if (node instanceof SyncComponent) {
                //System.out.println("QQQQ " + cnet.getName(node) + ".");
                qscombob.addItem(cnet.getName(node));
                cnt++;
            }
        }
        if (cnt > 1) {
            qscombob.addItem("ALL");
        } else {
            qscombob.addItem("NONE");
        }
    }

    void populateQslists(VisualXmas vnet, Xmas cnet) {
        int cnt = 0;
        SyncComponent sc;
        VisualSyncComponent vsc;

        if (cnt > 1) {
            qscombob.addItem("ALL");
        } else {
            qscombob.addItem("NONE");
        }
        for (Node node: vnet.getNodes()) {
            if (node instanceof VisualSyncComponent) {
                vsc = (VisualSyncComponent) node;
                sc = vsc.getReferencedSyncComponent();
                String rstr;
                rstr = cnet.getName(sc);
                rstr = rstr.replace(rstr.charAt(0), Character.toUpperCase(rstr.charAt(0)));
                qscombob.addItem(rstr);
                cnt++;
            }
        }
    }

    void createPanel(List<JPanel> panellist, Xmas cnet, VisualXmas vnet, int grnum) {
        panellist.add(new JPanel());
        panellist.get(panellist.size() - 1).add(new JLabel(" Sources" + ": "));
        panellist.get(panellist.size() - 1).add(mdcombob = new JComboBox());
        panellist.get(panellist.size() - 1).add(jcb = new JCheckBox(""));
        populateMd(grnum);
        ItemListener itemListener1 = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() instanceof JCheckBox) {
                    JCheckBox sjcb = (JCheckBox) e.getSource();
                    if (sjcb.isSelected()) {
                        index = jcbn.indexOf(sjcb) + 1;
                        //System.out.println("indexb==" + index);
                    }
                    if (jcblast != null) jcblast.setSelected(false);
                    jcblast = sjcb;
                    //String name = sjcb.getName();
                    //System.out.println(name);
                }
            }
        };
        jcb.addItemListener(itemListener1);
        jcbn.add(jcb);
        panellist.add(new JPanel());
        panellist.get(panellist.size() - 1).add(new JLabel(" Pt-to-pt" + ": "));
        panellist.get(panellist.size() - 1).add(q1combob = new JComboBox());
        panellist.get(panellist.size() - 1).add(q2combob = new JComboBox());
        populateQlists(cnet);
        panellist.get(panellist.size() - 1).add(jcb = new JCheckBox(""));
        ItemListener itemListener2 = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() instanceof JCheckBox) {
                    JCheckBox sjcb = (JCheckBox) e.getSource();
                    if (sjcb.isSelected()) {
                        index = jcbn.indexOf(sjcb) + 1;
                        //System.out.println("indexb==" + index);
                    }
                    if (jcblast != null) jcblast.setSelected(false);
                    jcblast = sjcb;
                    //String name = sjcb.getName();
                    //System.out.println(name);
                }
            }
        };
        jcb.addItemListener(itemListener2);
        jcbn.add(jcb);
        panellist.add(new JPanel());
        panellist.get(panellist.size() - 1).add(new JLabel(" Synchroniser" + ": "));
        panellist.get(panellist.size() - 1).add(qscombob = new JComboBox());
        populateQslists(vnet, cnet);
        panellist.get(panellist.size() - 1).add(jcb = new JCheckBox(""));
        ItemListener itemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() instanceof JCheckBox) {
                    JCheckBox sjcb = (JCheckBox) e.getSource();
                    if (sjcb.isSelected()) {
                        index = jcbn.indexOf(sjcb) + 1;
                        //System.out.println("indexb==" + index);
                    }
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
        System.out.println("Query is undergoing implemention");

        final VisualXmas vnet = WorkspaceUtils.getAs(we, VisualXmas.class);
        final Xmas xnet = WorkspaceUtils.getAs(we, Xmas.class);

        int grnum = Hierarchy.getDescendantsOfType(vnet.getRoot(), VisualGroup.class).size();

        mainFrame = new JFrame("Analysis");
        JPanel panelmain = new JPanel();
        mainFrame.getContentPane().add(panelmain, BorderLayout.PAGE_START);
        panelmain.setLayout(new BoxLayout(panelmain, BoxLayout.PAGE_AXIS));
        List<JPanel> panellist = new ArrayList<>();

        JPanel panela = new JPanel();
        panela.setLayout(new FlowLayout(FlowLayout.LEFT));
        panela.add(new JLabel(" QUERY [USE DEMO EXAMPLES] "));
        panela.add(Box.createHorizontalGlue());
        panelmain.add(panela);

        jcbn.clear();
        createPanel(panellist, xnet, vnet, grnum);
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

                    String s, str = "", str2 = "";
                    InputStreamReader inputStreamReader = new InputStreamReader(vxmProcess.getInputStream());
                    BufferedReader stdInput = new BufferedReader(inputStreamReader);
                    int n = 0;
                    int test = -1;
                    initHighlight(xnet, vnet);
                    while ((s = stdInput.readLine()) != null) {
                        if (test == -1) test = checkType(s);
                        if (n > 0) str = str + s + '\n';
                        n++;
                        System.out.println(s);
                    }
                    if (level.equals("advanced") && (q3flag == 0)) {
                        System.out.println("LEVEL IS ADVANCED ");
                        File qslFile = XmasSettings.getTempVxmQslFile();
                        processQsl(qslFile.getAbsolutePath());

                        File equFile1 = XmasSettings.getTempVxmEquFile();
                        str = processEq(equFile1.getAbsolutePath());

                        File queFile = XmasSettings.getTempVxmQueFile();
                        str2 = processQue(queFile.getAbsolutePath());
                    } else if (level.equals("advanced") && (q3flag == 1)) {
                        System.out.println("LEVEL IS ADVANCED ");
                        File equFile2 = XmasSettings.getTempVxmEquFile();
                        str = processEq(equFile2.getAbsolutePath());
                    } else if (level.equals("normal") && test == 2) {
                        System.out.println("LEVEL IS NORMAL ");
                        File locFile = XmasSettings.getTempVxmLocFile();
                        str = processLoc(locFile.getAbsolutePath());
                    }
                    if (test > 0) {
                        if (display.equals("popup")) {
                            if (!level.equals("advanced") && (q3flag == 0)) {
                                new SolutionsDialog1(test, str2);
                            } else if (level.equals("advanced") && (q3flag == 1)) {
                                new SolutionsDialog2(test, str);
                            } else {
                                new SolutionsDialog2(test, str2);
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
