package net.jorjai.modules.PingActivityStartModule;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import net.jorjai.bot.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ping_activity_start_data", indexes = {
        @Index(name = "idx_target_id", columnList = "target_id")
})
public class PingActivityStartData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "registry", nullable = false)
    private Long registry;

    @Column(name = "target_id", nullable = false)
    private long target_id;

    @Column(name = "game", nullable = false)
    private String game;

    @Column(name = "subscriber_id", nullable = false)
    private long subscriber_id;
}