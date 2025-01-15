package net.jorjai.handler.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.jorjai.handler.SlashCommand;
import net.jorjai.modules.PingActivityStartModule.PingActivityStartHandler;

import java.util.List;

public class UntrackCommand implements SlashCommand {
    @Override
    public void onSlashCommandEvent(SlashCommandInteractionEvent event) {
        // Thinking...
        event.deferReply(true).queue();

        // Get the target and subscriber ids
        long target_id = event.getOption("user").getAsUser().getIdLong();
        long subscriber_id = event.getUser().getIdLong();
        String game = event.getOption("game") != null ? event.getOption("game").getAsString() : "*";

        // Unregister the subscription
        PingActivityStartHandler.unregisterPing(target_id, game, subscriber_id);

        // Send the response
        event.getHook().sendMessage("You will no longer be notified when the user starts playing.").queue();
    }

    @Override
    public String getName() {
        return "untrack";
    }

    @Override
    public String getDescription() {
        return "Stop tracking a user playing a game";
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
                new OptionData(OptionType.USER, "user", "The user to stop tracking", true),
                new OptionData(OptionType.STRING, "game", "The game to stop tracking", false)
        );
    }
}
