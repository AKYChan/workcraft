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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.visual.SizeHelper;
import org.workcraft.gui.DesktopApi;
import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatPresetManager;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.mpsat.MpsatSettings.SolutionMode;
import org.workcraft.plugins.shared.gui.PresetManagerPanel;
import org.workcraft.plugins.shared.presets.Preset;
import org.workcraft.plugins.shared.presets.SettingsToControlsMapper;
import org.workcraft.util.GUI;

import info.clearthought.layout.TableLayout;

@SuppressWarnings("serial")
public class MpsatAssertionDialog extends JDialog {
    private JPanel predicatePanel, buttonsPanel;
    private PresetManagerPanel<MpsatSettings> presetPanel;
    private JTextArea assertionText;
    private final MpsatPresetManager presetManager;

    private int modalResult = 0;

    public MpsatAssertionDialog(Window owner, MpsatPresetManager presetManager) {
        super(owner, "Custom assertion", ModalityType.APPLICATION_MODAL);
        this.presetManager = presetManager;

        createPresetPanel();
        createAssertionPanel();
        createButtonsPanel();

        int buttonPanelHeight = buttonsPanel.getPreferredSize().height;
        int hGap = SizeHelper.getCompactLayoutHGap();
        int vGap = SizeHelper.getCompactLayoutVGap();
        double[][] size = new double[][] {
            {TableLayout.FILL},
            {TableLayout.PREFERRED, TableLayout.FILL, buttonPanelHeight},
        };

        final TableLayout layout = new TableLayout(size);
        layout.setHGap(hGap);
        layout.setVGap(vGap);

        JPanel contentPanel = new JPanel(layout);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(hGap, vGap, hGap, vGap));

        contentPanel.add(presetPanel, "0 0");
        contentPanel.add(predicatePanel, "0 1");
        contentPanel.add(buttonsPanel, "0 2");

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

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                assertionText.requestFocus();
            }
        });
        setMinimumSize(new Dimension(420, 350));
    }

    private void createPresetPanel() {
        ArrayList<Preset<MpsatSettings>> builtInPresets = new ArrayList<>();

        builtInPresets.add(new Preset<>("", MpsatSettings.getEmptyAssertionSettings(), false));

        SettingsToControlsMapper<MpsatSettings> guiMapper = new SettingsToControlsMapper<MpsatSettings>() {
            @Override
            public void applySettingsToControls(MpsatSettings settings) {
                MpsatAssertionDialog.this.applySettingsToControls(settings);
            }

            @Override
            public MpsatSettings getSettingsFromControls() {
                MpsatSettings settings = MpsatAssertionDialog.this.getSettingsFromControls();
                return settings;
            }
        };

        presetPanel = new PresetManagerPanel<MpsatSettings>(presetManager, builtInPresets, guiMapper, this);
    }

    private void createAssertionPanel() {
        predicatePanel = new JPanel(new BorderLayout());
        String title = "Assertion (use '" + NamespaceHelper.getFlatNameSeparator() + "' as hierarchy separator)";
        predicatePanel.setBorder(BorderFactory.createTitledBorder(title));

        assertionText = new JTextArea();
        assertionText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, SizeHelper.getMonospacedFontSize()));
        assertionText.setText("");
        assertionText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() > 127) {
                    e.consume();  // ignore non-ASCII characters
                }
            }
        });
        JScrollPane assertionScrollPane = new JScrollPane(assertionText);

        JPanel propertyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,
                SizeHelper.getCompactLayoutHGap(), SizeHelper.getCompactLayoutVGap()));

        predicatePanel.add(assertionScrollPane, BorderLayout.CENTER);
        predicatePanel.add(propertyPanel, BorderLayout.SOUTH);
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
                DesktopApi.open(new File("help/assertion.html"));
            }
        });

        buttonsPanel.add(runButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(helpButton);
    }

    public int getModalResult() {
        return modalResult;
    }

    private void applySettingsToControls(MpsatSettings settings) {
        assertionText.setText(settings.getExpression());
    }

    private MpsatSettings getSettingsFromControls() {
        return new MpsatSettings(null, MpsatMode.ASSERTION,
                0, SolutionMode.MINIMUM_COST, 0, assertionText.getText(), true);
    }

}
