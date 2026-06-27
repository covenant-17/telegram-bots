package dev.telegrambots.youtubemp3downloader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileNameSanitizerTest {
    @Test
    void testSanitizeRemovesGarbage() {
        assertEquals("Zillakami Lemon Juice", FileNameSanitizer.sanitize("ZillaKami - LEMON JUICE (Official Audio) (z2.fm) [320 kbps] #039;.mp3"));
        assertEquals("Lemon Juice", FileNameSanitizer.sanitize("LEMON JUICE (Official Audio)"));
        assertEquals("Artist Song Title", FileNameSanitizer.sanitize("Artist - Song Title (Official Music Video)"));
        assertEquals("Track 01", FileNameSanitizer.sanitize("Track 01 (lyric video)"));
        assertEquals("Hello World", FileNameSanitizer.sanitize("Hello & World #039;"));
    }

    @Test
    void testSanitizeHandlesNullAndEmpty() {
        assertNull(FileNameSanitizer.sanitize(null));
        assertEquals("", FileNameSanitizer.sanitize("   "));
    }

    @Test
    void testComposeFileNameNoDuplicateChannel() {
        String channel = FileNameSanitizer.sanitize("ZillaKami");
        String title = FileNameSanitizer.sanitize("ZillaKami - LEMON JUICE");
        assertEquals("Zillakami Lemon Juice", FileNameSanitizer.composeFileName(channel, title));
    }

    @Test
    void testComposeFileNameWithChannel() {
        String channel = FileNameSanitizer.sanitize("ZillaKami");
        String title = FileNameSanitizer.sanitize("LEMON JUICE");
        assertEquals("Zillakami - Lemon Juice", FileNameSanitizer.composeFileName(channel, title));
    }

    @Test
    void testComposeFileNameChannelBlank() {
        String channel = FileNameSanitizer.sanitize("");
        String title = FileNameSanitizer.sanitize("LEMON JUICE");
        assertEquals("Lemon Juice", FileNameSanitizer.composeFileName(channel, title));
    }

    @Test
    void testComposeFileNameTitleNull() {
        String channel = FileNameSanitizer.sanitize("ZillaKami");
        assertEquals("Zillakami", FileNameSanitizer.composeFileName(channel, null));
    }

    @Test
    void testSanitizeRemovesUnderscores() {
        assertEquals("Крыли Самозанятый L", FileNameSanitizer.sanitize("Крыли_Самозанятый_L_Премьера_Песни_2025.mp3"));
        assertEquals("Test Name", FileNameSanitizer.sanitize("Test__Name"));
        assertEquals("Test Name", FileNameSanitizer.sanitize("Test___Name"));
        assertEquals("Test Name", FileNameSanitizer.sanitize("Test_Name"));
    }

    @Test
    void testSanitizeRemovesKnownChannelAndGenreNoise() {
        assertEquals("♾ A Flock Of Seagulls I Ran Δllicθrn Remix",
                FileNameSanitizer.sanitize("Untitled Burial - ♾ A Flock Of Seagulls — I Ran Δllicθrn Remix.mp3"));
        assertEquals("Voco Void",
                FileNameSanitizer.sanitize("Voco Void Darkwave Post Punk.mp3"));
        assertEquals("System Of A Down Spiders Lara Newman Tuesday",
                FileNameSanitizer.sanitize("System Of A Down Spiders Lara Newman Tuesday Cover Darkwave Post Punk.mp3"));
        assertEquals("She Is A Purple Storm Alexandra Skye",
                FileNameSanitizer.sanitize("She Is A Purple Storm Darkwave Postpunk Indie Pop Alexandra Skye.mp3"));
        assertEquals("Nina Gallow Rosarote Brille",
                FileNameSanitizer.sanitize("Nina Gallow Rosarote Brille Minimal Synth Darkwave.mp3"));
        assertEquals("Alex Droidglow When The Sky Falls",
                FileNameSanitizer.sanitize("Aim To Head Release Alex Droidglow When The Sky Falls Original Mix.mp3"));
        assertEquals("Amanda Byron Absolute",
                FileNameSanitizer.sanitize("Amor Fati Amanda Byron Absolute.mp3"));
        assertEquals("Sisters Of Mercy Lucretia My Reflection",
                FileNameSanitizer.sanitize("RHINO Sisters of Mercy - Lucretia My Reflection (Official Music Video) [HD].mp3"));
        assertEquals("Maya Silver Nocturnal Animals",
                FileNameSanitizer.sanitize("Maya Silver Nocturnal Animals Cinematic Darkwave Noir Post Punk.mp3"));
        assertEquals("Maya Silver Nocturnal Animals",
                FileNameSanitizer.sanitize("Maya Silver Nocturnal Animals Сinematic Darkwave Noir Post Punk.mp3"));
        assertEquals("Maya Silver Heaven Or Hell",
                FileNameSanitizer.sanitize("Maya Silver Heaven Or Hell 𝗢𝗕𝗦𝗖𝗨𝗥𝗘 𝗩𝗘𝗥𝗦𝗜𝗢𝗡.mp3"));
        assertEquals("Amanda Byron Destruction",
                FileNameSanitizer.sanitize("Amor Fati - Amanda Byron Destruction.mp3"));
        assertEquals("Jane Ocean Mist Over The River",
                FileNameSanitizer.sanitize("Jane Ocean Mist Over The River Darkwave.mp3"));
        assertEquals("Lara Newman Just Another Girl In Her 20s",
                FileNameSanitizer.sanitize("Lara Newman Just Another Girl In Her 20s Darkwave Post Punk.mp3"));
        assertEquals("Lena Obscura Aquí Sigo Y Estoy Bien",
                FileNameSanitizer.sanitize("Lenaobscura - Lena Obscura Aquí Sigo Y Estoy Bien Deep.mp3"));
        assertEquals("Leonard Cohen Dance Me To The End Of Love Lara Newman",
                FileNameSanitizer.sanitize("Leonard Cohen Dance Me To The End Of Love Lara Newman Cover Darkwave Post Punk.mp3"));
        assertEquals("Maya Silver 7 Sins",
                FileNameSanitizer.sanitize("Maya Silver 7 Sins Cinematic Darkwave Noir Postpunk.mp3"));
        assertEquals("Mia Dark From My Bones",
                FileNameSanitizer.sanitize("Mia Dark From My Bones Dark Dream Pop.mp3"));
        assertEquals("Chernoburkv Циклон",
                FileNameSanitizer.sanitize("Nedostupnostь Chernoburkv Циклон.mp3"));
        assertEquals("Nina Gallow Nachtschwimmen",
                FileNameSanitizer.sanitize("Nina Gallow Nachtschwimmen Industrial Electronic.mp3"));
        assertEquals("Nina Gallow Supermarkt",
                FileNameSanitizer.sanitize("Nina Gallow Supermarkt Darkwave Minimal Synth.mp3"));
        assertEquals("Nina Gallow Tanz",
                FileNameSanitizer.sanitize("Nina Gallow Tanz Darkwave Postpunk.mp3"));
    }
}
