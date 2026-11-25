package gamemaster.cli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CliOptionsTest {

    @Test
    void noOptionsMeansTheGui() {
        CliOptions o = CliOptions.parse(new String[0]);
        assertFalse(o.cli);
        assertFalse(o.help);
        assertTrue(o.input == null);
    }

    @Test
    void inputStartsTheCliAndNamesTheFile() {
        CliOptions o = CliOptions.parse(new String[] {"-LLMAPI", "-input", "task.txt"});
        assertTrue(o.cli);
        assertEquals("task.txt", o.input);
        assertTrue(o.output == null);
    }

    @Test
    void outputAndCliFlagsAreRead() {
        CliOptions o = CliOptions.parse(new String[] {"-cli", "-output", "result.txt"});
        assertTrue(o.cli);
        assertTrue(o.input == null);
        assertEquals("result.txt", o.output);
    }

    @Test
    void helpIsRecognised() {
        assertTrue(CliOptions.parse(new String[] {"-help"}).help);
        assertTrue(CliOptions.USAGE.contains("-input <file>"));
    }

    @Test
    void badOptionsGiveReadableMessages() {
        IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> CliOptions.parse(new String[] {"-bogus"}));
        assertTrue(a.getMessage().contains("Unknown option: -bogus"));
        IllegalArgumentException b = assertThrows(IllegalArgumentException.class,
                () -> CliOptions.parse(new String[] {"-input"}));
        assertTrue(b.getMessage().contains("needs a file name"));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[] {"-input", "-cli"}));
    }
}
