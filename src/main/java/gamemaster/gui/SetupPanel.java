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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** The first screen: build a party of one to four heroes, pick a theme, and let the AI start the adventure. */
final class SetupPanel extends BackdropPanel {

    private final GameFacade game;
    private final Consumer<GeneratedScenario> onStarted;
    private final Runnable onLoaded;

    private final JComboBox<String> classBox = new JComboBox<>(new String[] {"Fighter", "Wizard", "Rogue"});
    private final JTextField nameField = new JTextField(14);
    private final JTextField themeField = new JTextField(18);
    private final JPanel partyRows = new JPanel();
    private final JLabel status = new JLabel(" ");
    private final StyledButton addButton = new StyledButton("Add", false);
    private final StyledButton customButton = new StyledButton("Custom stats...", false);
    private final StyledButton startButton = new StyledButton("Begin the adventure", true);
    private final StyledButton loadButton = new StyledButton("Load saved game", false);

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
        status.setForeground(GuiTheme.MUTED);
        add(status, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addQuick());
        nameField.addActionListener(e -> addQuick());
        customButton.addActionListener(e -> addCustom());
        startButton.addActionListener(e -> start());
        loadButton.addActionListener(e -> load());
        refreshParty();
    }

    private JComponent header() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel title = new JLabel("AI Game Master");
        title.setFont(GuiTheme.serif(java.awt.Font.BOLD, 42f));
        title.setForeground(GuiTheme.PURPLE_SOFT);
        JLabel sub = new JLabel("Gather your party. The game master will write the rest, and the dice will decide.");
        sub.setFont(GuiTheme.serif(java.awt.Font.ITALIC, 16f));
        sub.setForeground(GuiTheme.MUTED);
        p.add(title, BorderLayout.NORTH);
        p.add(sub, BorderLayout.SOUTH);
        return p;
    }

    private JComponent partyCard() {
        Surface card = new Surface(new BorderLayout(0, 12));
        card.add(sectionTitle("YOUR PARTY  (1 to 4 heroes)"), BorderLayout.NORTH);
        partyRows.setOpaque(false);
        partyRows.setLayout(new BoxLayout(partyRows, BoxLayout.Y_AXIS));
        card.add(partyRows, BorderLayout.CENTER);
        return card;
    }

    private JComponent rightColumn() {
        JPanel column = new JPanel(new GridLayout(2, 1, 0, 24));
        column.setOpaque(false);

        Surface create = new Surface(new BorderLayout(0, 12));
        create.add(sectionTitle("NEW HERO"), BorderLayout.NORTH);
        styleField(nameField);
        classBox.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        DarkUi.styleCombo(classBox);
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

        Surface adventure = new Surface(new BorderLayout(0, 12));
        adventure.add(sectionTitle("THE ADVENTURE"), BorderLayout.NORTH);
        styleField(themeField);
        JPanel themeRow = new JPanel(new BorderLayout(10, 0));
        themeRow.setOpaque(false);
        themeRow.add(label("Theme"), BorderLayout.WEST);
        themeRow.add(themeField, BorderLayout.CENTER);
        JPanel themeBox = new JPanel(new BorderLayout(0, 6));
        themeBox.setOpaque(false);
        themeBox.add(themeRow, BorderLayout.NORTH);
        JLabel hint = new JLabel("Optional, for example: haunted lighthouse, or leave it empty for a surprise.");
        hint.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        hint.setForeground(GuiTheme.MUTED);
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

    private void load() {
        List<String> saves = game.listSaves();
        if (saves.isEmpty()) {
            info("There are no saved games yet.");
            return;
        }
        Object choice = JOptionPane.showInputDialog(this, "Choose a saved game:", "Load game",
                JOptionPane.QUESTION_MESSAGE, null, saves.toArray(), saves.get(0));
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

    private void refreshParty() {
        partyRows.removeAll();
        List<Character> party = game.getParty();
        if (party.isEmpty()) {
            JLabel empty = new JLabel("<html>No heroes yet.<br>Add a name and a class on the right.</html>");
            empty.setFont(GuiTheme.serif(java.awt.Font.ITALIC, 15f));
            empty.setForeground(GuiTheme.MUTED);
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
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GuiTheme.SURFACE_ALT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(GuiTheme.PURPLE);
                g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(GuiTheme.padding(10, 16, 10, 12));
        JLabel name = new JLabel(c.getName() + "  the " + c.getCharacterClass().name());
        name.setFont(GuiTheme.serif(java.awt.Font.BOLD, 17f));
        StringBuilder stats = new StringBuilder();
        for (Stat s : Stat.values()) {
            stats.append(s.name(), 0, 3).append(' ').append(c.getStat(s)).append("  ");
        }
        JLabel vitals = new JLabel("HP " + c.getMaxHealth() + "    AC " + c.getArmorClass());
        vitals.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        vitals.setForeground(GuiTheme.PURPLE_SOFT);
        JLabel detail = new JLabel(stats.toString().trim());
        detail.setFont(GuiTheme.mono(java.awt.Font.PLAIN, 12f));
        detail.setForeground(GuiTheme.MUTED);
        JPanel lines = new JPanel(new BorderLayout(0, 2));
        lines.setOpaque(false);
        lines.add(vitals, BorderLayout.NORTH);
        lines.add(detail, BorderLayout.SOUTH);
        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        text.add(name, BorderLayout.NORTH);
        text.add(lines, BorderLayout.SOUTH);
        StyledButton remove = new StyledButton("Remove", false);
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
        status.setForeground(GuiTheme.PURPLE_SOFT);
        status.setText(message);
    }

    private void info(String message) {
        status.setForeground(GuiTheme.MUTED);
        status.setText(message);
    }

    private void error(String message) {
        status.setForeground(GuiTheme.ERROR);
        status.setText(message);
    }

    private static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(GuiTheme.sans(java.awt.Font.BOLD, 12f));
        l.setForeground(GuiTheme.PURPLE_SOFT);
        return l;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        l.setForeground(GuiTheme.MUTED);
        l.setPreferredSize(new Dimension(52, 20));
        return l;
    }

    private static void styleField(JTextField f) {
        f.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        f.setBorder(GuiTheme.fieldBorder());
        f.setPreferredSize(new Dimension(200, 38));
    }
}
