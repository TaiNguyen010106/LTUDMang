package vn.edu.nptbrowser.ui;

import vn.edu.nptbrowser.config.AppConfig;
import vn.edu.nptbrowser.util.NetworkUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

public final class SettingsDialog extends JDialog {
    private boolean saved;

    public SettingsDialog(BrowserFrame owner, AppConfig config) {
        super(owner, "Cau hinh NPT Browser", Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));

        JTextField homeField = new JTextField(config.homeUrl(), 36);
        JTextField apiField = new JTextField(config.apiBaseUrl(), 36);
        JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(config.timeoutSeconds(), 1, 300, 1));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 8, 6, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Trang chu IIS:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        form.add(homeField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        form.add(new JLabel("Backend API:"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        form.add(apiField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0;
        form.add(new JLabel("Timeout (giay):"), constraints);
        constraints.gridx = 1;
        form.add(timeoutSpinner, constraints);
        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Huy");
        cancel.addActionListener(event -> dispose());
        JButton save = new JButton("Luu");
        save.addActionListener(event -> {
            try {
                String home = NetworkUtils.normalizeHttpUrl(homeField.getText());
                String api = NetworkUtils.normalizeHttpUrl(apiField.getText());
                config.updateUserSettings(home, api, (Integer) timeoutSpinner.getValue());
                saved = true;
                dispose();
            } catch (IllegalArgumentException | IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Khong luu duoc", JOptionPane.ERROR_MESSAGE);
            }
        });
        actions.add(cancel);
        actions.add(save);
        add(actions, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(save);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public boolean wasSaved() {
        return saved;
    }
}
