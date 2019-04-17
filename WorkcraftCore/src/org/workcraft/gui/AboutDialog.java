package org.workcraft.gui;

import org.workcraft.Info;
import org.workcraft.dom.visual.SizeHelper;
import org.workcraft.utils.DesktopApi;
import org.workcraft.utils.GuiUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private boolean modalResult;

    public AboutDialog(final MainWindow owner) {
        super(owner);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setTitle("About");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ok();
            }
        });

        BufferedImage logoImage = null;
        JLabel logoLabel;
        try {
            logoImage = GuiUtils.loadImageFromResource("images/logo.png");
            logoLabel = new JLabel(new ImageIcon(logoImage), SwingConstants.CENTER);
        } catch (IOException e) {
            logoLabel = new JLabel("Workcraft", SwingConstants.CENTER);
            Font font = logoLabel.getFont();
            logoLabel.setFont(font.deriveFont(72.0f));
        }
        int borderSize = SizeHelper.getLayoutHGap() + SizeHelper.getLayoutVGap();
        logoLabel.setBorder(BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize));

        JEditorPane infoPane = new JEditorPane();
        infoPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        infoPane.setFocusable(false);

        infoPane.setContentType("text/html");

        infoPane.setEditable(false);
        infoPane.setOpaque(false);

        String homepage = Info.getHomepage();
        infoPane.setText("<p center>" + Info.getFullTitle() + "</p>"
                + "<p center>" + Info.getCopyright() + "</p>"
                + "<p center><a href='" + homepage + "'>" + homepage + "</a></p>");

        infoPane.addHyperlinkListener(event -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) {
                try {
                    URI uri = event.getURL().toURI();
                    DesktopApi.browse(uri);
                } catch (URISyntaxException e) {
                    System.out.println(e);
                }
            }
        });

        JButton okButton = GuiUtils.createDialogButton("OK");
        okButton.addActionListener(event -> ok());

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.CENTER, SizeHelper.getLayoutHGap(), SizeHelper.getLayoutVGap()));
        buttonsPane.add(okButton);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(SizeHelper.getLayoutHGap(), SizeHelper.getLayoutVGap()));
        contentPane.add(logoLabel, BorderLayout.NORTH);
        contentPane.add(infoPane, BorderLayout.CENTER);
        contentPane.add(buttonsPane, BorderLayout.SOUTH);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        getRootPane().registerKeyboardAction(event -> ok(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().registerKeyboardAction(event -> ok(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setModal(true);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    private void ok() {
        modalResult = true;
        setVisible(false);
    }

    public boolean reveal() {
        setVisible(true);
        return modalResult;
    }

}
