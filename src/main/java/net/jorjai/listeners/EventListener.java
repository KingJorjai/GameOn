package net.jorjai.listeners;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.jorjai.modules.PingActivityStartModule.PingActivityStartHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

public class EventListener extends ListenerAdapter {

    @Override
    public void onUserActivityStart(@NotNull UserActivityStartEvent event){
        LoggerFactory.getLogger(EventListener.class).debug("{} started activity {}", event.getUser().getName(), event.getNewActivity().getName());

        PingActivityStartHandler.onUserActivityStart(event);
    }

    @Override
    public void onUserActivityEnd(@NotNull UserActivityEndEvent event) {
        LoggerFactory.getLogger(EventListener.class).debug("{} stopped activity {}", event.getUser().getName(), event.getOldActivity().getName());

        PingActivityStartHandler.onUserActivityEnd(event);
    }
}
