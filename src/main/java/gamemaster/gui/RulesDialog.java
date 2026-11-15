package gamemaster.gui;

import gamemaster.app.RulesText;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** The rules breakdown: a scrollable pixel-style window that opens from the title menu and closes with a button or Esc. */
final class RulesDialog extends PixelDialog {

    RulesDialog(java.awt.Window owner) {
        super(owner, "RULES");
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        for (RulesText.Section s : RulesText.sections()) {
            PixelLabel heading = new PixelLabel(s.title(), 2, PixelTheme::hilite);
            heading.setAlignmentX(0f);
            heading.setBorder(GuiTheme.padding(14, 0, 6, 0));
            list.add(heading);
            for (String p : s.paragraphs()) {
                JLabel text = new JLabel(WrappedText.html(p, 400));
                text.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
                text.setForeground(PixelTheme.body());
                text.setAlignmentX(0f);
                text.setBorder(GuiTheme.padding(0, 0, 6, 0));
                list.add(text);
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        PixelUi.styleScroll(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(590, 430));
        body().add(scroll, BorderLayout.CENTER);
        PixelButton close = new PixelButton("CLOSE", true);
        close.addActionListener(e -> dispose());
        setButtons(close);
        getRootPane().setDefaultButton(close);
    }
}
