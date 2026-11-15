package gamemaster.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Optional;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/** Ready-made pixel-style replacements for the JOptionPane prompts the app used. */
final class PixelDialogs {

    private PixelDialogs() {}

    private static JLabel prompt(String text) {
        JLabel l = new JLabel(WrappedText.html(text, 380));
        l.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        l.setForeground(PixelTheme.body());
        return l;
    }

    /** Asks for a line of text; empty when cancelled. */
    static Optional<String> input(Component parent, String title, String message, String initial) {
        PixelDialog d = new PixelDialog(PixelDialog.ownerOf(parent), title);
        JTextField field = new JTextField(initial == null ? "" : initial, 24);
        PixelUi.styleField(field);
        d.body().add(prompt(message), BorderLayout.NORTH);
        d.body().add(field, BorderLayout.CENTER);
        String[] result = {null};
        PixelButton ok = new PixelButton("SAVE", true);
        PixelButton cancel = new PixelButton("CANCEL", false);
        ok.addActionListener(e -> {
            result[0] = field.getText();
            d.dispose();
        });
        cancel.addActionListener(e -> d.dispose());
        field.addActionListener(e -> ok.doClick());
        d.setButtons(cancel, ok);
        d.getRootPane().setDefaultButton(ok);
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) { field.requestFocusInWindow(); }
        });
        d.open(parent);
        return Optional.ofNullable(result[0]);
    }

    /** Lets the player pick one of {@code choices}; empty when cancelled. */
    static Optional<String> choose(Component parent, String title, String message, List<String> choices,
                                   String okText) {
        PixelDialog d = new PixelDialog(PixelDialog.ownerOf(parent), title);
        JList<String> list = new JList<>(choices.toArray(new String[0]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setOpaque(false);
        list.setFixedCellHeight(34);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                    boolean selected, boolean focus) {
                String text = String.valueOf(value);
                return new JComponent() {
                    @Override public Dimension getPreferredSize() { return new Dimension(300, 34); }
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        if (selected) {
                            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.30f));
                            g2.setColor(PixelTheme.hiliteFill());
                            g2.fillRect(0, 0, getWidth(), getHeight());
                            g2.setComposite(java.awt.AlphaComposite.SrcOver);
                            g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                            PixelFont.paint(g2, ">", 8, (getHeight() - PixelFont.height(2)) / 2, 2, PixelTheme.hilite());
                        }
                        g2.setFont(GuiTheme.sans(java.awt.Font.BOLD, 15f));
                        g2.setColor(selected ? PixelTheme.hilite() : PixelTheme.body());
                        g2.drawString(text, 32, (getHeight() + g2.getFontMetrics().getAscent()
                                - g2.getFontMetrics().getDescent()) / 2);
                        g2.dispose();
                    }
                };
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        PixelUi.styleScroll(scroll);
        scroll.setPreferredSize(new Dimension(380, Math.min(7, Math.max(3, choices.size())) * 34 + 8));
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(PixelTheme.field());
        box.setBorder(new javax.swing.border.LineBorder(PixelTheme.edge(), 2));
        box.add(scroll);
        d.body().add(prompt(message), BorderLayout.NORTH);
        d.body().add(box, BorderLayout.CENTER);
        String[] result = {null};
        PixelButton ok = new PixelButton(okText, true);
        PixelButton cancel = new PixelButton("CANCEL", false);
        ok.addActionListener(e -> {
            result[0] = list.getSelectedValue();
            d.dispose();
        });
        cancel.addActionListener(e -> d.dispose());
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    ok.doClick();
                }
            }
        });
        d.setButtons(cancel, ok);
        d.getRootPane().setDefaultButton(ok);
        d.open(parent);
        return Optional.ofNullable(result[0]);
    }

    /** Shows a message with one button per option; returns the index chosen, or -1 when dismissed with Esc. */
    static int confirm(Component parent, String title, String message, String... options) {
        PixelDialog d = new PixelDialog(PixelDialog.ownerOf(parent), title);
        d.body().add(prompt(message), BorderLayout.CENTER);
        int[] result = {-1};
        PixelButton[] buttons = new PixelButton[options.length];
        for (int i = 0; i < options.length; i++) {
            int index = i;
            buttons[i] = new PixelButton(options[i].toUpperCase(), i == 0);
            buttons[i].addActionListener(e -> {
                result[0] = index;
                d.dispose();
            });
        }
        d.setButtons(buttons);
        d.getRootPane().setDefaultButton(buttons[0]);
        d.open(parent);
        return result[0];
    }

    /** Shows a message or any component with a single OK button. */
    static void message(Component parent, String title, JComponent content) {
        PixelDialog d = new PixelDialog(PixelDialog.ownerOf(parent), title);
        d.body().add(content, BorderLayout.CENTER);
        PixelButton ok = new PixelButton("OK", true);
        ok.addActionListener(e -> d.dispose());
        d.setButtons(ok);
        d.getRootPane().setDefaultButton(ok);
        d.open(parent);
    }

    static void message(Component parent, String title, String text) {
        message(parent, title, prompt(text));
    }
}
