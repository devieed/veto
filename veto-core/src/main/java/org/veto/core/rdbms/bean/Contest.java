package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

// 赛事
@Entity
@Table(name = "contests", indexes = {
        @Index(columnList = "cn_name"),
        @Index(columnList = "en_name"),
        @Index(columnList = "status"),
        @Index(columnList = "created_at"),
        @Index(columnList = "home_team_id"),
        @Index(columnList = "away_team_id"),
        @Index(columnList = "total_bet"),
        @Index(columnList = "start_time"),
        @Index(columnList = "cn_name,en_name,home_team_id,away_team_id,start_time", unique = true)
})
@Setter
@Getter
public class Contest implements VetoBean{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cnName;

    private String enName;

    @Lob
    private String icon;

    @Enumerated(EnumType.ORDINAL)
    private STATUS status;
    // 总买入下注量
    @Column(name = "total_bet", precision = 18, scale = 2)
    private BigDecimal totalBet;

    // 主队
    @Column(name = "home_team_id")
    private Integer homeTeamId;
    // 客队
    @Column(name = "away_team_id")
    private Integer awayTeamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", referencedColumnName = "id", updatable = false, insertable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", referencedColumnName = "id", updatable = false, insertable = false)
    private Team awayTeam;

    @OneToMany(mappedBy = "contest", fetch = FetchType.LAZY)
    private List<ScoreOdds> scoreOdds;

    // 开始时间
    private Date startTime;
    // 结束时间
    private Date endTime;

    private Date createdAt;

    private Date updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = new Date();
        this.createdAt = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    // 状态 ， 一般是球赛的状态
    public enum STATUS{
        PENDING, // 未开始
        ACTIVE, // 进行中
        ENDED, // 已结束
        CLOSE, // 比赛关闭，需要退款
        DELETE,
    }
}
