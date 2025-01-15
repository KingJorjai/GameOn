package net.jorjai.modules.PingActivityStartModule;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.jorjai.bot.Bot;

import java.util.List;
import java.util.Objects;

public class PingActivityStartHandler {
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
        // Return if activity is not playing
//        if (event.getNewActivity().getType() != Activity.ActivityType.PLAYING) {
//            return;
//        }

        // Get the target id and game
        long target_id = event.getUser().getIdLong();
        String game = Objects.requireNonNull(event.getNewActivity()).getName();
        String target_username = event.getUser().getName();
        String activity_type = event.getNewActivity().getType().toString().toLowerCase();


        // create the embed
        MessageEmbed embed = new EmbedBuilder()
                .setColor(0x69ffc9)
                .setAuthor(target_username + " is now " + activity_type + " " + game, null, event.getUser().getAvatarUrl())
                .build();

        // Look for subscribers and send them the message
        EntityManager em = Bot.getEmf().createEntityManager();

        Query query = em.createQuery("SELECT p FROM PingActivityStartData p WHERE p.target_id = :targetId AND (p.game = :game OR p.game = '*')");
        query.setParameter("targetId", target_id);
        query.setParameter("game", game);

        List<PingActivityStartData> result = query.getResultList();
        em.close();

        for (PingActivityStartData data : result) {
            event.getJDA().getUserById(data.getSubscriber_id()).openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessageEmbeds(embed).queue();
            });
        }
    }
}
