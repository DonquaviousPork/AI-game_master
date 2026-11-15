package gamemaster.gui;

import gamemaster.ai.GeneratedScenario;
import gamemaster.app.GameFacade;
import gamemaster.model.Character;
import gamemaster.model.Stat;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** The first screen: build a party of one to four heroes, pick a theme, and let the AI start the adventure. */
final class SetupPanel extends PixelBackdrop {

    private final GameFacade game;
    private final Consumer<GeneratedScenario> onStarted;
    private final Runnable onLoaded;

    private final JComboBox<String> classBox = new JComboBox<>(new String[] {"Fighter", "Wizard", "Rogue"});
    private final JTextField nameField = new JTextField(14);
    private final JTextField themeField = new JTextField(18);
    private final JPanel partyRows = new JPanel();
    private final JLabel status = new JLabel(" ");
    private final PixelButton addButton = new PixelButton("ADD", false);
    private final PixelButton customButton = new PixelButton("CUSTOM STATS...", false);
    private final PixelButton startButton = new PixelButton("BEGIN THE ADVENTURE", true);
    private final PixelButton loadButton = new PixelButton("LOAD SAVED GAME", false);
    private final PixelButton backButton = new PixelButton("BACK TO TITLE", false);
    private Runnable onBack = () -> { };

    SetupPanel(GameFacade game, Consumer<GeneratedScenario> onStarted, Runnable onLoaded) {
        super(new BorderLayout(0, 22));
        this.game = game;
        this.onStarted = onStarted;
        this.onLoaded = onLoaded;
        setBorder(GuiTheme.padding(30, 38, 24, 38));

        add(header(), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 24, 0));
        columns.setOpaque(false);
        columns.add(partyCard());
        columns.add(rightColumn());
        add(columns, BorderLayout.CENTER);

        status.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 13f));
        status.setForeground(PixelTheme.muted());
        JPanel south = new JPanel(new BorderLayout(12, 0));
        south.setOpaque(false);
        south.add(status, BorderLayout.CENTER);
        south.add(backButton, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addQuick());
        nameField.addActionListener(e -> addQuick());
        customButton.addActionListener(e -> addCustom());
        startButton.addActionListener(e -> start());
        loadButton.addActionListener(e -> load());
        backButton.addActionListener(e -> onBack.run());
        refreshParty();
    }

    private JComponent header() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        PixelLabel title = new PixelLabel("AI GAME MASTER", 5, PixelTheme::hilite, () -> new java.awt.Color(0xFF9F43));
        JLabel sub = new JLabel("Gather your party. The game master will write the rest, and the dice will decide.");
        sub.setFont(GuiTheme.serif(java.awt.Font.ITALIC, 16f));
        sub.setForeground(PixelTheme.text());
        p.add(title, BorderLayout.NORTH);
        p.add(sub, BorderLayout.SOUTH);
        return p;
    }

    private JComponent partyCard() {
        PixelPanel card = new PixelPanel(new BorderLayout(0, 12));
        card.add(sectionTitle("YOUR PARTY (1 TO 4 HEROES)"), BorderLayout.NORTH);
        partyRows.setOpaque(false);
        partyRows.setLayout(new BoxLayout(partyRows, BoxLayout.Y_AXIS));
        card.add(partyRows, BorderLayout.CENTER);
        return card;
    }

    private JComponent rightColumn() {
        JPanel column = new JPanel(new GridLayout(2, 1, 0, 24));
        column.setOpaque(false);

        PixelPanel create = new PixelPanel(new BorderLayout(0, 12));
        create.add(sectionTitle("NEW HERO"), BorderLayout.NORTH);
        PixelUi.styleField(nameField);
        classBox.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        PixelUi.styleCombo(classBox);
        classBox.setPreferredSize(new Dimension(200, 38));
        JPanel form = new JPanel(new GridLayout(2, 1, 0, 12));
        form.setOpaque(false);
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);
        row1.add(label("Name"), BorderLayout.WEST);
        row1.add(nameField, BorderLayout.CENTER);
        JPanel row2 = new JPanel(new BorderLayout(10, 0));
        row2.setOpaque(false);
        row2.add(label("Class"), BorderLayout.WEST);
        row2.add(classBox, BorderLayout.CENTER);
        form.add(row1);
        form.add(row2);
        JPanel formTop = new JPanel(new BorderLayout());
        formTop.setOpaque(false);
        formTop.add(form, BorderLayout.NORTH);
        create.add(formTop, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(customButton);
        create.add(buttons, BorderLayout.SOUTH);

        PixelPanel adventure = new PixelPanel(new BorderLayout(0, 12));
        adventure.add(sectionTitle("THE ADVENTURE"), BorderLayout.NORTH);
        PixelUi.styleField(themeField);
        JPanel themeRow = new JPanel(new BorderLayout(10, 0));
        themeRow.setOpaque(false);
        themeRow.add(label("Theme"), BorderLayout.WEST);
        themeRow.add(themeField, BorderLayout.CENTER);
        JPanel themeBox = new JPanel(new BorderLayout(0, 6));
        themeBox.setOpaque(false);
        themeBox.add(themeRow, BorderLayout.NORTH);
        JLabel hint = new JLabel("Optional, for example: haunted lighthouse, or leave it empty for a surprise.");
        hint.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        hint.setForeground(PixelTheme.muted());
        themeBox.add(hint, BorderLayout.CENTER);
        JPanel themeTop = new JPanel(new BorderLayout());
        themeTop.setOpaque(false);
        themeTop.add(themeBox, BorderLayout.NORTH);
        adventure.add(themeTop, BorderLayout.CENTER);
        JPanel go = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        go.setOpaque(false);
        go.add(startButton);
        go.add(loadButton);
        adventure.add(go, BorderLayout.SOUTH);

        column.add(create);
        column.add(adventure);
        return column;
    }

    // ---- actions ----

    private void addQuick() {
        try {
            game.addCharacter(nameField.getText(), (String) classBox.getSelectedItem());
            afterAdd();
        } catch (RuntimeException e) {
            error(Background.describe(e));
        }
    }

    private void addCustom() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            error("Give the hero a name first.");
            return;
        }
        CustomCharacterDialog.ask(this, name).ifPresent(points -> {
            try {
                game.addCustomCharacter(name, (String) classBox.getSelectedItem(), points);
                afterAdd();
            } catch (RuntimeException e) {
                error(Background.describe(e));
            }
        });
    }

    private void afterAdd() {
        nameField.setText("");
        nameField.requestFocusInWindow();
        info(" ");
        refreshParty();
    }

    private void start() {
        setBusy(true, "The game master is writing your adventure...");
        String theme = themeField.getText();
        Background.run(() -> game.startAdventure(theme), scenario -> {
            setBusy(false, " ");
            onStarted.accept(scenario);
        }, t -> {
            setBusy(false, " ");
            error(Background.describe(t));
        });
    }

    /** Called by the title screen to return to it. */
    void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    /** Asks which saved game to load and loads it; also used by the title screen's Load game. */
    void load() {
        List<String> saves = game.listSaves();
        if (saves.isEmpty()) {
            info("There are no saved games yet.");
            return;
        }
        Object choice = PixelDialogs.choose(this, "LOAD GAME", "Choose a saved game:", saves, "LOAD").orElse(null);
        if (choice == null) {
            return;
        }
        try {
            game.load((String) choice);
            onLoaded.run();
        } catch (RuntimeException e) {
            error(Background.describe(e));
        }
    }

    private void remove(Character c) {
        try {
            game.removeCharacter(c.getName());
            info(" ");
            refreshParty();
        } catch (RuntimeException e) {
            error(Background.describe(e));
        }
    }

    // ---- display ----

    /** Called when the player comes back from a game: shows the (now empty) party list again. */
    void showMenu() {
        status.setText(" ");
        refreshParty();
    }

    private void refreshParty() {
        partyRows.removeAll();
        List<Character> party = game.getParty();
        if (party.isEmpty()) {
            JLabel empty = new JLabel("<html>No heroes yet.<br>Add a name and a class on the right.</html>");
            empty.setFont(GuiTheme.serif(java.awt.Font.ITALIC, 15f));
            empty.setForeground(PixelTheme.muted());
            partyRows.add(empty);
        }
        for (Character c : party) {
            partyRows.add(row(c));
            partyRows.add(javax.swing.Box.createVerticalStrut(10));
        }
        startButton.setEnabled(!party.isEmpty());
        addButton.setEnabled(party.size() < 4);
        customButton.setEnabled(party.size() < 4);
        partyRows.revalidate();
        partyRows.repaint();
    }

    private JComponent row(Character c) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setColor(PixelTheme.field());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(PixelTheme.edge());
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(PixelTheme.hiliteFill());
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(GuiTheme.padding(10, 16, 10, 12));
        JLabel name = new JLabel(c.getName() + "  the " + c.getCharacterClass().name());
        name.setFont(GuiTheme.serif(java.awt.Font.BOLD, 17f));
        name.setForeground(PixelTheme.body());
        StringBuilder stats = new StringBuilder();
        for (Stat s : Stat.values()) {
            stats.append(s.name(), 0, 3).append(' ').append(c.getStat(s)).append("  ");
        }
        JLabel vitals = new JLabel("HP " + c.getMaxHealth() + "    AC " + c.getArmorClass());
        vitals.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        vitals.setForeground(PixelTheme.hilite());
        JLabel detail = new JLabel(stats.toString().trim());
        detail.setFont(GuiTheme.mono(java.awt.Font.PLAIN, 12f));
        detail.setForeground(PixelTheme.muted());
        JPanel lines = new JPanel(new BorderLayout(0, 2));
        lines.setOpaque(false);
        lines.add(vitals, BorderLayout.NORTH);
        lines.add(detail, BorderLayout.SOUTH);
        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        text.add(name, BorderLayout.NORTH);
        text.add(lines, BorderLayout.SOUTH);
        PixelButton remove = new PixelButton("REMOVE", false);
        remove.addActionListener(e -> remove(c));
        row.add(text, BorderLayout.CENTER);
        row.add(remove, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void setBusy(boolean busy, String message) {
        addButton.setEnabled(!busy && game.getParty().size() < 4);
        customButton.setEnabled(!busy && game.getParty().size() < 4);
        startButton.setEnabled(!busy && !game.getParty().isEmpty());
        loadButton.setEnabled(!busy);
        nameField.setEnabled(!busy);
        themeField.setEnabled(!busy);
        classBox.setEnabled(!busy);
        status.setForeground(PixelTheme.hilite());
        status.setText(message);
    }

    private void info(String message) {
        status.setForeground(PixelTheme.text());
        status.setText(message);
    }

    private void error(String message) {
        status.setForeground(PixelTheme.bad());
        status.setText(message);
    }

    private static JComponent sectionTitle(String text) {
        return new PixelLabel(text, 2, PixelTheme::hilite);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        l.setForeground(PixelTheme.text());
        l.setPreferredSize(new Dimension(52, 20));
        return l;
    }
}
