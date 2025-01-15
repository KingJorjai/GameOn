package net.jorjai.handler.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.jorjai.handler.SlashCommand;
import net.jorjai.modules.PingActivityStartModule.PingActivityStartHandler;

import java.util.List;
import java.util.Objects;

public class TrackCommand implements SlashCommand {
    @Override
    public void onSlashCommandEvent(SlashCommandInteractionEvent event) {
        // Thinking...
        event.deferReply(true).queue();

        // Get the target, game and subscriber ids
        long target_id = Objects.requireNonNull(event.getOption("user")).getAsUser().getIdLong();
        long subscriber_id = event.getUser().getIdLong();
        String game = event.getOption("game") != null ? Objects.requireNonNull(event.getOption("game")).getAsString() : "*";

        // Register the subscription
        PingActivityStartHandler.registerPing(target_id, game, subscriber_id);

        // Send the response
        event.getHook().sendMessage("You will be notified when the user starts playing.").queue();
    }

    @Override
    public String getName() {
        return "track";
    }

    @Override
    public String getDescription() {
        return "Track a user playing a game";
    }

    @Override
    public boolean isSpecificGuild() {
        return true;
    }

    @Override
    public boolean isGuildOnly() {
        return false;
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "The user to track", true),
                new OptionData(OptionType.STRING, "game", "The game to track", false)
                );
    }
}
