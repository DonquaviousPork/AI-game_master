package gamemaster.gui;

import gamemaster.ai.GeneratedScenario;
import gamemaster.ai.TurnResult;
import gamemaster.app.GameFacade;
import gamemaster.game.GameState;
import gamemaster.game.GameView;
import gamemaster.model.Monster;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * The play screen: the story and the rules log in the middle, hero and enemy cards on the right, and
 * the action box at the bottom. It is the Observer in the design: it registers as a GameView and redraws
 * the cards and phase whenever the game changes. Model calls run in the background, so it never freezes.
 */
final class GamePanel extends BackdropPanel implements GameView {

    private final GameFacade game;

    private final JLabel title = new JLabel(" ");
    private final PhasePill phase = new PhasePill();
    private final JTextPane story = new JTextPane();
    private final JPanel sidebar = new JPanel();
    private final JLabel status = new JLabel(" ");
    private final JTextField input = new JTextField();
    private final StyledButton send = new StyledButton("Act", true);
    private final StyledButton undo = new StyledButton("Undo", false);
    private final StyledButton save = new StyledButton("Save", false);
    private final StyledButton load = new StyledButton("Load", false);

    private boolean busy;
    private boolean over;
    private boolean endingShown;
    private boolean unsaved;

    GamePanel(GameFacade game) {
        super(new BorderLayout(0, 14));
        this.game = game;
        setBorder(GuiTheme.padding(22, 28, 20, 28));

        add(header(), BorderLayout.NORTH);
        add(centre(), BorderLayout.CENTER);
        add(bottom(), BorderLayout.SOUTH);

        send.addActionListener(e -> send());
        input.addActionListener(e -> send());
        undo.addActionListener(e -> undo());
        save.addActionListener(e -> save());
        load.addActionListener(e -> load());
        game.attach(this);
        applyEnabled();
    }

    // ---- Observer ----

    /** May be called from the background thread; the copy is made here and drawn on the screen thread. */
    @Override
    public void update(GameState state) {
        GameViewModel model = GameViewModel.from(state);
        SwingUtilities.invokeLater(() -> render(model));
    }

    private void render(GameViewModel m) {
        title.setText(m.title().isEmpty() ? " " : m.title());
        phase.setPhase(m.phase());
        over = m.over();
        sidebar.removeAll();
        sidebar.add(sidebarTitle("PARTY"));
        sidebar.add(Box.createVerticalStrut(6));
        for (GameViewModel.Card c : m.heroes()) {
            sidebar.add(new CombatantCard(c, GuiTheme.PURPLE));
            sidebar.add(Box.createVerticalStrut(8));
        }
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(sidebarTitle("ENEMIES"));
        sidebar.add(Box.createVerticalStrut(6));
        boolean any = false;
        for (GameViewModel.Card c : m.enemies()) {
            sidebar.add(new CombatantCard(c, GuiTheme.HEALTH_LOW));
            sidebar.add(Box.createVerticalStrut(8));
            any = true;
        }
        if (!any) {
            JLabel none = new JLabel("None in sight");
            none.setFont(GuiTheme.serif(java.awt.Font.ITALIC, 14f));
            none.setForeground(GuiTheme.MUTED);
            none.setAlignmentX(LEFT_ALIGNMENT);
            sidebar.add(none);
        }
        sidebar.revalidate();
        sidebar.repaint();
        if (over && !endingShown) {
            endingShown = true;
            paragraph("The story has ended.", Style.ENDING);
            setStatus("The story has ended. You can load a saved game.", GuiTheme.PURPLE_SOFT);
        }
        applyEnabled();
    }

    // ---- entry points from the app ----

    /** Called after a new adventure was created. */
    void begin(GeneratedScenario s) {
        unsaved = false;
        clearStory();
        endingShown = false;
        paragraph(s.scenario().description(), Style.NARRATION);
        paragraph(s.openingEvent().description(), Style.SCENE);
        for (Monster m : s.monsters()) {
            paragraph("Enemy: " + m.getName() + "  (HP " + m.getMaxHealth() + ", AC " + m.getArmorClass() + ")", Style.RULES);
        }
        setStatus("What do you do?", GuiTheme.PURPLE_SOFT);
        render(GameViewModel.from(game.getState()));
        input.requestFocusInWindow();
    }

    /** Called after a saved game was loaded. */
    void resume() {
        unsaved = false;
        clearStory();
        endingShown = false;
        GameState s = game.getState();
        paragraph("Game loaded.", Style.RULES);
        if (s.getCurrentEvent() != null) {
            paragraph(s.getCurrentEvent().description(), Style.NARRATION);
        }
        List<String> log = s.getLog();
        for (String line : log.subList(Math.max(0, log.size() - 4), log.size())) {
            paragraph(line, Style.RULES);
        }
        setStatus("What do you do?", GuiTheme.PURPLE_SOFT);
        render(GameViewModel.from(s));
    }

    // ---- actions ----

    private void send() {
        String text = input.getText().trim();
        if (text.isEmpty() || busy || over) {
            return;
        }
        paragraph("You: " + text, Style.PLAYER);
        input.setText("");
        unsaved = true;
        busy = true;
        applyEnabled();
        setStatus("The game master is thinking...", GuiTheme.PURPLE_SOFT);
        Background.run(() -> game.submitAction(text), this::showTurn, t -> {
            busy = false;
            setStatus(Background.describe(t), GuiTheme.ERROR);
            applyEnabled();
            input.requestFocusInWindow();
        });
    }

    private void showTurn(TurnResult r) {
        paragraph(r.narration(), Style.NARRATION);
        for (String line : r.toolActivity()) {
            paragraph("[rules] " + line, Style.RULES);
        }
        busy = false;
        setStatus(r.gameOver() ? "The story has ended." : "What do you do?", GuiTheme.MUTED);
        applyEnabled();
        input.requestFocusInWindow();
    }

    private void undo() {
        unsaved = true;
        try {
            setStatus(game.undo()
                    ? "Last action undone. Damage and fleeing are reversed; dice are not re-rolled."
                    : "Nothing to undo.", GuiTheme.MUTED);
        } catch (RuntimeException e) {
            setStatus(Background.describe(e), GuiTheme.ERROR);
        }
    }

    /** Asks for a name and saves. Returns true only if the game was actually saved. */
    boolean save() {
        Object name = JOptionPane.showInputDialog(this, "Name this save (letters, digits, - and _):",
                "Save game", JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return false;
        }
        try {
            game.save(name.toString().trim());
            unsaved = false;
            setStatus("Saved as \"" + name.toString().trim() + "\".", GuiTheme.HEALTH_OK);
            return true;
        } catch (RuntimeException e) {
            setStatus(Background.describe(e), GuiTheme.ERROR);
            return false;
        }
    }

    /** True when the player has acted or undone something since the game was started, loaded or saved. */
    boolean hasUnsavedProgress() { return unsaved; }

    void markUnsaved() { unsaved = true; }

    private void load() {
        List<String> saves = game.listSaves();
        if (saves.isEmpty()) {
            setStatus("There are no saved games yet.", GuiTheme.MUTED);
            return;
        }
        Object choice = JOptionPane.showInputDialog(this, "Choose a saved game:", "Load game",
                JOptionPane.QUESTION_MESSAGE, null, saves.toArray(), saves.get(0));
        if (choice == null) {
            return;
        }
        try {
            game.load((String) choice);
            resume();
        } catch (RuntimeException e) {
            setStatus(Background.describe(e), GuiTheme.ERROR);
        }
    }

    // ---- layout ----

    private JComponent header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        title.setFont(GuiTheme.serif(java.awt.Font.BOLD, 26f));
        title.setForeground(GuiTheme.PURPLE_SOFT);
        p.add(title, BorderLayout.WEST);
        p.add(phase, BorderLayout.EAST);
        return p;
    }

    private JComponent centre() {
        JPanel p = new JPanel(new BorderLayout(18, 0));
        p.setOpaque(false);

        story.setEditable(false);
        story.setOpaque(true);
        story.setBackground(GuiTheme.SURFACE);
        story.setBorder(GuiTheme.padding(18, 22, 18, 22));
        story.setCaretColor(GuiTheme.SURFACE);
        ((DefaultCaret) story.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane storyScroll = new JScrollPane(story);
        storyScroll.setBorder(javax.swing.BorderFactory.createLineBorder(GuiTheme.BORDER, 1, true));
        storyScroll.getViewport().setBackground(GuiTheme.SURFACE);
        DarkUi.styleScrollPane(storyScroll);
        p.add(storyScroll, BorderLayout.CENTER);

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);
        JPanel sidebarHolder = new JPanel(new BorderLayout());
        sidebarHolder.setOpaque(false);
        sidebarHolder.add(sidebar, BorderLayout.NORTH);
        JScrollPane side = new JScrollPane(sidebarHolder);
        side.setOpaque(false);
        side.getViewport().setOpaque(false);
        side.setBorder(null);
        side.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        DarkUi.styleScrollPane(side);
        side.setPreferredSize(new Dimension(300, 0));
        p.add(side, BorderLayout.EAST);
        return p;
    }

    private JComponent bottom() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        status.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 13f));
        status.setForeground(GuiTheme.MUTED);
        p.add(status, BorderLayout.NORTH);

        input.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 15f));
        input.setBorder(GuiTheme.fieldBorder());
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(input, BorderLayout.CENTER);
        row.add(send, BorderLayout.EAST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(undo);
        buttons.add(save);
        buttons.add(load);
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(row, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void applyEnabled() {
        input.setEnabled(!busy && !over);
        send.setEnabled(!busy && !over);
        undo.setEnabled(!busy && !over);
        save.setEnabled(!busy && !over);
        load.setEnabled(!busy);
    }

    private void setStatus(String text, Color colour) {
        status.setForeground(colour);
        status.setText(text);
    }

    private static JLabel sidebarTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(GuiTheme.sans(java.awt.Font.BOLD, 12f));
        l.setForeground(GuiTheme.MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    // ---- story text ----

    private enum Style { HEADING, NARRATION, SCENE, PLAYER, RULES, ENDING }

    /** One paragraph of the story as it was written, kept so the screen can be redrawn in another colour mode. */
    record StoryEntry(String text, String style) {}

    private final List<StoryEntry> entries = new java.util.ArrayList<>();

    private void clearStory() {
        story.setText("");
        entries.clear();
    }

    /** The story so far, for carrying across a rebuild of the window. */
    List<StoryEntry> exportStory() { return List.copyOf(entries); }

    /** Redraws a story exported from an earlier panel, with the current colours. */
    void importStory(List<StoryEntry> saved, boolean unsavedProgress) {
        clearStory();
        for (StoryEntry e : saved) {
            paragraph(e.text(), Style.valueOf(e.style()));
        }
        endingShown = saved.stream().anyMatch(e -> e.style().equals(Style.ENDING.name()));
        unsaved = unsavedProgress;
        setStatus(over ? "The story has ended." : "What do you do?", GuiTheme.MUTED);
        render(GameViewModel.from(game.getState()));
    }

    boolean isBusy() { return busy; }

    /** Stops listening to the game; used when the window is rebuilt and this panel is replaced. */
    void detach() { game.getState().detach(this); }

    private void paragraph(String text, Style style) {
        entries.add(new StoryEntry(text, style.name()));
        StyledDocument doc = story.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet();
        switch (style) {
            case HEADING -> {
                StyleConstants.setFontFamily(a, GuiTheme.serif(0, 12).getFamily());
                StyleConstants.setFontSize(a, 24);
                StyleConstants.setBold(a, true);
                StyleConstants.setForeground(a, GuiTheme.PURPLE_SOFT);
                StyleConstants.setSpaceBelow(a, 8f);
            }
            case NARRATION -> {
                StyleConstants.setFontFamily(a, GuiTheme.serif(0, 12).getFamily());
                StyleConstants.setFontSize(a, 17);
                StyleConstants.setForeground(a, GuiTheme.TEXT);
                StyleConstants.setLineSpacing(a, 0.25f);
                StyleConstants.setSpaceBelow(a, 12f);
            }
            case SCENE -> {
                StyleConstants.setFontFamily(a, GuiTheme.serif(0, 12).getFamily());
                StyleConstants.setFontSize(a, 17);
                StyleConstants.setItalic(a, true);
                StyleConstants.setForeground(a, GuiTheme.SCENE_TEXT);
                StyleConstants.setLineSpacing(a, 0.25f);
                StyleConstants.setSpaceBelow(a, 12f);
            }
            case PLAYER -> {
                StyleConstants.setFontFamily(a, GuiTheme.sans(0, 12).getFamily());
                StyleConstants.setFontSize(a, 15);
                StyleConstants.setBold(a, true);
                StyleConstants.setForeground(a, GuiTheme.PURPLE);
                StyleConstants.setSpaceAbove(a, 10f);
                StyleConstants.setSpaceBelow(a, 8f);
            }
            case RULES -> {
                StyleConstants.setFontFamily(a, GuiTheme.mono(0, 12).getFamily());
                StyleConstants.setFontSize(a, 13);
                StyleConstants.setForeground(a, GuiTheme.RULES);
                StyleConstants.setSpaceBelow(a, 4f);
            }
            case ENDING -> {
                StyleConstants.setFontFamily(a, GuiTheme.serif(0, 12).getFamily());
                StyleConstants.setFontSize(a, 19);
                StyleConstants.setBold(a, true);
                StyleConstants.setForeground(a, GuiTheme.PURPLE_SOFT);
                StyleConstants.setSpaceAbove(a, 12f);
            }
        }
        try {
            int start = doc.getLength();
            doc.insertString(start, text + "\n", a);
            doc.setParagraphAttributes(start, text.length() + 1, a, false);
            story.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
            // appending at the end can not be out of range
        }
    }

    /** A small rounded label showing the current game phase. */
    private static final class PhasePill extends JLabel {
        PhasePill() {
            setFont(GuiTheme.sans(java.awt.Font.BOLD, 12f));
            setForeground(GuiTheme.TEXT);
            setBorder(GuiTheme.padding(6, 16, 6, 16));
            setOpaque(false);
        }

        void setPhase(String phase) {
            setText(phase.toUpperCase());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean combat = "COMBAT".equals(getText());
            g2.setColor(combat ? GuiTheme.COMBAT_FILL : GuiTheme.PURPLE_DARK);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(combat ? GuiTheme.HEALTH_LOW : GuiTheme.PURPLE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
