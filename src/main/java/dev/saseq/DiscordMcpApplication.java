package dev.saseq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DiscordMcpApplication {
    public static void main(String[] args) {
        // Terminate when the stdio client detaches, otherwise JDA's non-daemon
        // threads keep the JVM (and the --rm container) alive forever.
        System.setIn(new ExitOnEofInputStream(System.in));
        SpringApplication.run(DiscordMcpApplication.class, args);
    }
}
