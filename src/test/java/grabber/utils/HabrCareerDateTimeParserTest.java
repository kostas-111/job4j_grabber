package grabber.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HabrCareerDateTimeParserTest {
    @Test
    public void whenParseDate() {
        HabrCareerDateTimeParser parser = new HabrCareerDateTimeParser();
        String date = "2024-10-17T10:44:29+03:00";
        String expected = "2024-10-17T10:44:29";
        assertThat(parser.parse(date)).isEqualTo(expected);
    }
}