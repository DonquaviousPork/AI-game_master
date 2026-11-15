package gamemaster.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * A window in the title screen's style: no system title bar, a framed plate, pixel lettering for the title and
 * the buttons. Drag it by its plate; Esc closes it. Fill {@link #body()} and call {@link #setButtons}.
 */
class PixelDialog extends JDialog {

    private final JPanel body = new JPanel(new BorderLayout(0, 10));
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel plate = new JPanel(new BorderLayout(0, 14)) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            PixelUi.frame(g2, getWidth(), getHeight(), 1f);
            g2.dispose();
        }
    };

    PixelDialog(Window owner, String title) {
        super(owner, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        plate.setOpaque(true);
        plate.setBorder(GuiTheme.padding(20, 26, 18, 26));

        PixelLabel heading = new PixelLabel(title, title.length() > 16 ? 2 : 3, PixelTheme::hilite);
        JPanel head = new JPanel(new BorderLayout(0, 10));
        head.setOpaque(false);
        head.add(heading, BorderLayout.NORTH);
        head.add(new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(10, 3); }
            @Override protected void paintComponent(Graphics g) {
                g.setColor(PixelTheme.edge());
                g.fillRect(0, 0, getWidth(), 2);
                g.setColor(PixelTheme.corner());
                g.fillRect(0, 0, 36, 2);
            }
        }, BorderLayout.SOUTH);

        body.setOpaque(false);
        buttons.setOpaque(false);
        plate.add(head, BorderLayout.NORTH);
        plate.add(body, BorderLayout.CENTER);
        plate.add(buttons, BorderLayout.SOUTH);
        setContentPane(plate);

        MouseAdapter drag = new MouseAdapter() {
            private Point from;
            @Override public void mousePressed(MouseEvent e) { from = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - from.x, p.y + e.getY() - from.y);
            }
        };
        plate.addMouseListener(drag);
        plate.addMouseMotionListener(drag);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cancelled(); }
        });
        setResizable(false);
    }

    /** Called when the player presses Esc; the default simply closes. */
    void cancelled() { dispose(); }

    JPanel body() { return body; }

    void setButtons(PixelButton... list) {
        buttons.removeAll();
        for (PixelButton b : list) {
            buttons.add(b);
        }
    }

    /** Sizes the dialog, puts it over {@code parent} and shows it (blocks until closed). */
    void open(Component parent) {
        pack();
        if (getWidth() < 380) {
            setSize(380, getHeight());
        }
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    static Window ownerOf(Component parent) {
        return parent == null ? null : javax.swing.SwingUtilities.getWindowAncestor(parent);
    }
}
