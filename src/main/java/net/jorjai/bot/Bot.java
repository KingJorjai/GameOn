package net.jorjai.bot;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.jorjai.handler.SlashCommandHandler;

import java.util.Objects;

public class Bot {

    @Getter
    private final Dotenv config; //environment variables

    @Getter
    private final JDA jda;

    @Getter
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("ping_activity_start_data");

    public Bot() throws InvalidTokenException, InterruptedException {
        config = Dotenv.configure().ignoreIfMissing().load(); // Load the environment variables
        String token = config.get("TOKEN"); // Get the token from the environment variables
        String id = config.get("GUILD_ID"); // Get the guild id from the environment variables

        // Create a new builder
        JDABuilder builder =
                JDABuilder.createDefault(token); // Create a new builder with the token
        builder
                .setStatus(OnlineStatus.ONLINE) // Set the status of the bot to online
                .setActivity(Activity.playing("GameOn")); // Set the activity of the bot to watching "KingJorjai"
        builder.enableIntents(
            //GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_PRESENCES
        );
        builder.setMemberCachePolicy(MemberCachePolicy.ALL) // Which members to cache
                .setChunkingFilter(ChunkingFilter.ALL) // Load all members at startup
                .enableCache(
                        CacheFlag.ACTIVITY // Needed for member activity update
                );


        jda = builder.build(); // Build the bot in the jda

        // Register listeners
        jda.awaitReady().addEventListener(
                new SlashCommandHandler(jda, Objects.requireNonNull(jda.getGuildById(id))),
                new net.jorjai.listeners.EventListener()
        );
    }

    public static void main(String[] args) {

        try {
            new Bot(); // Create a new bot
        } catch (InvalidTokenException e) {
            System.err.println("ERROR: Invalid token provided! Please provide a valid token.");
        } catch (InterruptedException e) {
            System.err.println("ERROR: Bot was interrupted while starting up!");
        }
    }
}
