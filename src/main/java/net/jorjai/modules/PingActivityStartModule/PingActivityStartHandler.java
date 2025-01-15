package net.jorjai.modules.PingActivityStartModule;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.jorjai.bot.Bot;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PingActivityStartHandler {

    private static Map<Long,List<String>> activeUsers = Collections.synchronizedMap(new HashMap<>());

    public static boolean registerPing (long target_id, String game, long subscriber_id) {
        EntityManager em = Bot.getEmf().createEntityManager();
        EntityTransaction et = em.getTransaction();

        // Check if the subscription already exists
        Query query = em.createQuery("SELECT COUNT(p) FROM PingActivityStartData p WHERE p.target_id = :targetId AND (p.game = :game OR p.game = '*') AND p.subscriber_id = :subscriberId");
        if ((long) query.setParameter("targetId", target_id).setParameter("game", game).setParameter("subscriberId", subscriber_id).getSingleResult() > 0) {
            return false;
        }

        // Remove other subscriptions when game is the wildcard
        if (game.equals("*")) {
            query = em.createQuery("DELETE FROM PingActivityStartData p WHERE p.target_id = :targetId AND p.subscriber_id = :subscriberId");
            et.begin();
            query.setParameter("targetId", target_id);
            query.setParameter("subscriberId", subscriber_id);
            query.executeUpdate();
            et.commit();
        } else {
            // Check if the user is already subscribed to the wildcard
            query = em.createQuery("SELECT COUNT(p) FROM PingActivityStartData p WHERE p.target_id = :targetId AND p.game = '*' AND p.subscriber_id = :subscriberId");
            if ((long) query.setParameter("targetId", target_id).setParameter("subscriberId", subscriber_id).getSingleResult() > 0) {
                // Ignore the new subscription
                return false;
            }
        }

        // Create the data
        PingActivityStartData data = new PingActivityStartData();
        data.setTarget_id(target_id);
        data.setGame(game);
        data.setSubscriber_id(subscriber_id);

        // Save the data
        et.begin();
        em.persist(data);
        et.commit();
        em.close();

        return true;
    }

    public static boolean unregisterPing (long target_id, String game, long subscriber_id) {
        EntityManager em = Bot.getEmf().createEntityManager();
        em.getTransaction().begin();

        // Check if there is no matching subscription
        Query query = em.createQuery("SELECT COUNT(p) FROM PingActivityStartData p WHERE p.target_id = :targetId AND (p.game = :game OR p.game = '*') AND p.subscriber_id = :subscriberId");
        if ((long) query.setParameter("targetId", target_id).setParameter("game", game).setParameter("subscriberId", subscriber_id).getSingleResult() == 0) {
            return false;
        }
        if (game.equals("*")) {
            query = em.createQuery("DELETE FROM PingActivityStartData p WHERE p.target_id = :targetId AND p.subscriber_id = :subscriberId");
        } else {
            query = em.createQuery("DELETE FROM PingActivityStartData p WHERE p.target_id = :targetId AND p.game = :game AND p.subscriber_id = :subscriberId");
            query.setParameter("game", game);
        }
        query.setParameter("targetId", target_id);
        query.setParameter("subscriberId", subscriber_id);
        query.executeUpdate();
        em.getTransaction().commit();
        em.close();

        return true;
    }

    public static void onUserActivityStart(UserActivityStartEvent event){

        long targetId = event.getUser().getIdLong();
        Activity.ActivityType activityType = event.getNewActivity().getType();
        String game = event.getNewActivity().getName();
        String targetUsername = event.getUser().getName();

        // Return if the user is already in the active users list or the activity is a custom status
        if (
            (activeUsers.containsKey(targetId) && activeUsers.get(targetId).contains(game)) ||  // Avoid multiple calls from each server
            activityType == Activity.ActivityType.CUSTOM_STATUS                                 // Ignore custom status
        ) {
            LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Ignoring activity {} start event for user {}", game, targetUsername);
            return;
        }

        // Add the user to the active users list or add the activity to the user
        if (!activeUsers.containsKey(targetId)) {
            activeUsers.put(targetId, new LinkedList<>(List.of(game)));
            LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Added user {} to the active users list with activity {}", targetUsername, game);
        } else {
            activeUsers.get(targetId).add(game);
            LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Added activity {} to user {}", game, targetUsername);
        }

        // Retrieve the game and the target username
        String action = activityTypeToActionString(activityType);


        // Create the embed
        MessageEmbed embed = new EmbedBuilder()
                .setColor(0x69ffc9)
                .setAuthor(targetUsername + " is now " + action + " " + game, null, event.getUser().getAvatarUrl())
                .build();

        // Look for subscribers to this game or all
        EntityManager em = Bot.getEmf().createEntityManager();
        Query query = em.createQuery("SELECT p FROM PingActivityStartData p WHERE p.target_id = :targetId AND (p.game = :game OR p.game = '*')");
        query.setParameter("targetId", targetId);
        query.setParameter("game", game);
        List<PingActivityStartData> result = query.getResultList();
        em.close();

        // Send the message to the subscribers
        for (PingActivityStartData data : result) {
            event.getJDA().getUserById(data.getSubscriber_id()).openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessageEmbeds(embed).queue();
                LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Sent activity {} start message to user {}", game, targetUsername);
            });
        }
    }

    public static void onUserActivityEnd(UserActivityEndEvent event) {
        long targetId = event.getUser().getIdLong();
        String targetUsername = event.getUser().getName();
        String game = event.getOldActivity().getName();
        String action = activityTypeToActionString(event.getOldActivity().getType());

        // Return if the user is not in the active users list to avoid duplicates
        if (!activeUsers.containsKey(targetId) || !activeUsers.get(targetId).contains(game)) {
            LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Ignoring activity {} end event for user {}", event.getOldActivity().getName(), targetUsername);
            return;
        }

        // Remove the activity from the user
        activeUsers.get(targetId).remove(event.getOldActivity().getName());
        LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Removed activity {} from user {}", event.getOldActivity().getName(), targetUsername);

        // Create the embed
        MessageEmbed embed = new EmbedBuilder()
                .setColor(0xff7088)
                .setAuthor(targetUsername + " has stopped " + action + " " + game, null, event.getUser().getAvatarUrl())
                .build();

        // Look for subscribers to this game or all
        EntityManager em = Bot.getEmf().createEntityManager();
        Query query = em.createQuery("SELECT p FROM PingActivityStartData p WHERE p.target_id = :targetId AND (p.game = :game OR p.game = '*')");
        query.setParameter("targetId", targetId);
        query.setParameter("game", game);
        List<PingActivityStartData> result = query.getResultList();
        em.close();

        // Send the message to the subscribers
        for (PingActivityStartData data : result) {
            event.getJDA().getUserById(data.getSubscriber_id()).openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessageEmbeds(embed).queue();
                LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Sent activity {} start message to user {}", game, targetUsername);
            });
        }

        // If the user has no more activities, remove it from the active users list
        if (activeUsers.get(targetId).isEmpty()) {
            activeUsers.remove(targetId);
            LoggerFactory.getLogger(PingActivityStartHandler.class).debug("Removed user {} from the active users list", targetUsername);
        }
    }


    /**
     * Converts an activity type to a string that describes the action
     *
     * @param activityType The activity type
     * @return The string that describes the action
     */
    private static String activityTypeToActionString(Activity.ActivityType activityType) {
        String action = "doing something";
        switch (activityType){
            case COMPETING -> {
                action = "competing in";
            }
            case CUSTOM_STATUS -> {
                action = "using a custom status";
            }
            case LISTENING -> {
                action = "listening to";
            }
            case PLAYING -> {
                action = "playing";
            }
            case STREAMING -> {
                action = "streaming";
            }
            case WATCHING -> {
                action = "watching";
            }
        }
        return action;
    }
}
