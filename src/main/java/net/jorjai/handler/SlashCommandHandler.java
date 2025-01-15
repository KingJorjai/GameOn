package net.jorjai.handler;

import io.github.classgraph.ClassGraph;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlashCommandHandler extends ListenerAdapter {
    private static final Map<String, SlashCommand> slashMap = new HashMap<>();
    private final CommandListUpdateAction globalCommandsData;
    private final CommandListUpdateAction guildCommandsData;


    public SlashCommandHandler(JDA jda, Guild guild) {

        this.globalCommandsData = jda.updateCommands();
        this.guildCommandsData = guild.updateCommands();

        ClassGraph classGraph = new ClassGraph();
        List<SlashCommand> slashCommands = new ArrayList<>();

        classGraph.enableClassInfo()
                .scan()
                .getClassesImplementing(SlashCommand.class)
                .forEach(classInfo -> {
                    try {
                        SlashCommand slashCommand = (SlashCommand) classInfo.loadClass().getDeclaredConstructor().newInstance();
                        slashCommands.add(slashCommand);
                    } catch (RuntimeException | InvocationTargetException | InstantiationException |
                             IllegalAccessException | NoSuchMethodException e) {
                        throw new RuntimeException("Unable to add Slash with the reason " + e);
                    }
                });
        classGraph.scan().close();

        registerSlashCommands(slashCommands);
    }

    private void registerSlashCommand(SlashCommand slashCommand) {
        slashMap.put(slashCommand.getName(), slashCommand);

        if (slashCommand.isSpecificGuild()) {
            guildCommandsData.addCommands(slashCommand.getCommandData());
        } else {
            globalCommandsData.addCommands(slashCommand.getCommandData());
        }
    }

    public void registerSlashCommands(List<SlashCommand> slashCommandList) {
        slashCommandList.forEach(this::registerSlashCommand);

        queueCommands();
    }

    private void queueCommands() {
        globalCommandsData.queue();
        guildCommandsData.queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand slashCommand = slashMap.get(event.getName());

        slashCommand.onSlashCommandEvent(event);
    }
}
