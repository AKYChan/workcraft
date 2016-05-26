package org.workcraft.plugins.mpsat.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.gui.DesktopApi;
import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatPresetManager;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.mpsat.MpsatSettings.SolutionMode;
import org.workcraft.plugins.shared.gui.PresetManagerPanel;
import org.workcraft.plugins.shared.presets.Preset;
import org.workcraft.plugins.shared.presets.SettingsToControlsMapper;
import org.workcraft.util.GUI;
import org.workcraft.util.IntDocument;

import info.clearthought.layout.TableLayout;

@SuppressWarnings("serial")
public class MpsatConfigurationDialog extends JDialog {
    private JPanel optionsPanel, predicatePanel, buttonsPanel;
    private PresetManagerPanel<MpsatSettings> presetPanel;
    private JComboBox<MpsatMode> modeCombo;
    private JTextField solutionLimitText;
    private JTextArea reachText;
    private JRadioButton allSolutionsRadioButton, firstSolutionRadioButton, cheapestSolutionRadioButton;
    private JRadioButton unsatisfiebleRadioButton;
    private final MpsatPresetManager presetManager;

    private final TableLayout layout;
    private int modalResult = 0;

    public MpsatConfigurationDialog(Window owner, MpsatPresetManager presetManager) {
        super(owner, "Custom property definition", ModalityType.APPLICATION_MODAL);
        this.presetManager = presetManager;

        createPresetPanel();
        createOptionsPanel();
        createReachPanel();
        createButtonsPanel();

        double[][] size = new double[][] {
                {TableLayout.FILL},
                {TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL, buttonsPanel.getPreferredSize().height},
        };

        layout = new TableLayout(size);
        layout.setHGap(3);
        layout.setVGap(3);

        JPanel contentPanel = new JPanel(layout);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        contentPanel.add(presetPanel, "0 0");
        contentPanel.add(optionsPanel, "0 1");
        contentPanel.add(predicatePanel, "0 2");
        contentPanel.add(buttonsPanel, "0 3");

        setContentPane(contentPanel);

        presetPanel.selectFirst();

        getRootPane().registerKeyboardAction(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        modalResult = 0;
                        setVisible(false);
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setMinimumSize(new Dimension(420, 350));
    }

    private void createPresetPanel() {
        ArrayList<Preset<MpsatSettings>> builtInPresets = new ArrayList<>();

        if (presetManager.isAllowStgPresets()) {
            builtInPresets.add(new Preset<>("Consistency",
                    MpsatSettings.getConsistencySettings(), true));

            builtInPresets.add(new Preset<>("Delay insensitive interface",
                    MpsatSettings.getDiInterfaceSettings(), true));

            builtInPresets.add(new Preset<>("Input properness",
                    MpsatSettings.getInputPropernessSettings(), true));

            builtInPresets.add(new Preset<>("Output persistency (without dummies)",
                    MpsatSettings.getOutputPersistencySettings(), true));
        }

        builtInPresets.add(new Preset<>("Deadlock freeness",
                MpsatSettings.getDeadlockReachSettings(), true));

        SettingsToControlsMapper<MpsatSettings> guiMapper = new SettingsToControlsMapper<MpsatSettings>() {
            @Override
            public void applySettingsToControls(MpsatSettings settings) {
                MpsatConfigurationDialog.this.applySettingsToControls(settings);
            }

            @Override
            public MpsatSettings getSettingsFromControls() {
                MpsatSettings settings = MpsatConfigurationDialog.this.getSettingsFromControls();
                return settings;
            }
        };

        presetPanel = new PresetManagerPanel<MpsatSettings>(presetManager, builtInPresets, guiMapper, this);
    }

    private void createOptionsPanel() {
        optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder("MPSat settings"));

        modeCombo = new JComboBox<MpsatMode>();
        modeCombo.setEditable(false);
        if (presetManager.isAllowStgPresets()) {
            modeCombo.addItem(MpsatMode.STG_REACHABILITY);
        }
        modeCombo.addItem(MpsatMode.REACHABILITY);

        optionsPanel.add(GUI.createWideLabeledComponent(modeCombo, "Mode:      "), BorderLayout.NORTH);

        JPanel solutionModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        solutionModePanel.add(new JLabel("Solution:"));
        cheapestSolutionRadioButton = new JRadioButton("minimise cost function");
        cheapestSolutionRadioButton.setSelected(true);
        cheapestSolutionRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                solutionLimitText.setEnabled(false);
            }
        });
        firstSolutionRadioButton = new JRadioButton("any");
        firstSolutionRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                solutionLimitText.setEnabled(false);
            }
        });
        allSolutionsRadioButton = new JRadioButton("all");
        allSolutionsRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                solutionLimitText.setEnabled(true);
            }
        });
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(cheapestSolutionRadioButton);
        buttonGroup.add(firstSolutionRadioButton);
        buttonGroup.add(allSolutionsRadioButton);
        solutionModePanel.add(cheapestSolutionRadioButton);
        solutionModePanel.add(firstSolutionRadioButton);
        solutionModePanel.add(allSolutionsRadioButton);

        solutionLimitText = new JTextField();
        Dimension dimension = solutionLimitText.getPreferredSize();
        dimension.width = 3 * dimension.height;
        solutionLimitText.setPreferredSize(dimension);
        solutionLimitText.setToolTipText("Maximum number of solutions. Leave blank for no limit.");
        solutionLimitText.setDocument(new IntDocument(3));
        solutionLimitText.setEnabled(false);
        solutionModePanel.add(solutionLimitText);
        optionsPanel.add(solutionModePanel, BorderLayout.SOUTH);
    }

    private void createReachPanel() {
        predicatePanel = new JPanel(new BorderLayout());
        String title = "Reach predicate (use '" + NamespaceHelper.getFlatNameSeparator() + "' as hierarchy separator)";
        predicatePanel.setBorder(BorderFactory.createTitledBorder(title));

        reachText = new JTextArea();
        reachText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        reachText.setText("");
        reachText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() > 127) {
                    e.consume();  // ignore non-ASCII characters
                }
            }
        });
        JScrollPane reachScrollPane = new JScrollPane(reachText);

        JRadioButton satisfiebleRadioButton = new JRadioButton("satisfiable");
        unsatisfiebleRadioButton = new JRadioButton("unsatisfiable");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(satisfiebleRadioButton);
        buttonGroup.add(unsatisfiebleRadioButton);
        unsatisfiebleRadioButton.setSelected(true);

        JPanel propertyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        propertyPanel.add(new JLabel("Property holds if predicate is:"));
        propertyPanel.add(satisfiebleRadioButton);
        propertyPanel.add(unsatisfiebleRadioButton);

        predicatePanel.add(reachScrollPane, BorderLayout.CENTER);
        predicatePanel.add(propertyPanel, BorderLayout.SOUTH);
    }

    private void applySettingsToControls(MpsatSettings settings) {
        modeCombo.setSelectedItem(settings.getMode());

        switch (settings.getSolutionMode()) {
        case MINIMUM_COST:
            cheapestSolutionRadioButton.setSelected(true);
            solutionLimitText.setEnabled(false);
            break;
        case FIRST:
            firstSolutionRadioButton.setSelected(true);
            solutionLimitText.setEnabled(false);
            break;
        case ALL:
            allSolutionsRadioButton.setSelected(true);
            solutionLimitText.setEnabled(true);
            break;
        }

        int n = settings.getSolutionNumberLimit();
        if (n > 0) {
            solutionLimitText.setText(Integer.toString(n));
        } else {
            solutionLimitText.setText("");
        }

        reachText.setText(settings.getReach());
        String propertyName = settings.getName();
        if (propertyName == null) {
            propertyName = "";
        }
        unsatisfiebleRadioButton.setSelected(settings.getInversePredicate());
    }

    public MpsatSettings getSettings() {
        return getSettingsFromControls();
    }

    private void createButtonsPanel() {
        buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton runButton = GUI.createDialogButton("Run");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modalResult = 1;
                setVisible(false);
            }
        });

        JButton cancelButton = GUI.createDialogButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modalResult = 0;
                setVisible(false);
            }
        });

        JButton helpButton = GUI.createDialogButton("Help");
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DesktopApi.open(new File("help/reach.html"));
            }
        });

        buttonsPanel.add(runButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(helpButton);
    }

    private MpsatSettings getSettingsFromControls() {
        SolutionMode solutionMode;
        if (firstSolutionRadioButton.isSelected()) {
            solutionMode = SolutionMode.FIRST;
        } else if (cheapestSolutionRadioButton.isSelected()) {
            solutionMode = SolutionMode.MINIMUM_COST;
        } else {
            solutionMode = SolutionMode.ALL;
        }

        int solutionLimin;
        try {
            solutionLimin = Integer.parseInt(solutionLimitText.getText());
        } catch (NumberFormatException e) {
            solutionLimin = 0;
        }
        if (solutionLimin < 0) {
            solutionLimin = 0;
        }

        MpsatSettings settings = new MpsatSettings(null, (MpsatMode) modeCombo.getSelectedItem(),
                0, solutionMode, solutionLimin, reachText.getText(), unsatisfiebleRadioButton.isSelected());

        return settings;
    }

    public int getModalResult() {
        return modalResult;
    }
}
