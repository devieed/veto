package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.veto.shared.ODD_SCORE;

import java.math.BigDecimal;
import java.util.Date;
/**
 * 比分盈利率
 */
@Entity
@Table(name = "score_odds", indexes = {
        @Index(columnList = "contest_id"),
        @Index(columnList = "contest_id,`score`,is_half", unique = true),
        @Index(columnList = "is_half"),
        @Index(columnList = "`score`"),
        @Index(columnList = "score_str"),
        @Index(columnList = "odds"),
        @Index(columnList = "ticket")
})
@Setter
@Getter
public class ScoreOdds implements VetoBean{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", insertable = false, updatable = false, referencedColumnName = "id")
    private Contest contest;

    // 比分 格式：主队比分-客队比分
    @Enumerated(EnumType.ORDINAL)
    private ODD_SCORE score;
    // 只有结果可能会用到，当score为other的时候
    private String scoreStr;

    private Date createdAt;

    private Date updatedAt;

    // 全场还是半场 false：全场 true：半场
    private Boolean isHalf;

    @Column(precision = 6, scale = 5)
    private BigDecimal odds;

    private Boolean ticket;

    @Enumerated(EnumType.ORDINAL)
    private STATUS status;


    public enum STATUS{
        WAIT_DRAWN, // 待开奖
        REWARD_DRAWN, // 已发放奖励
        NO_TICKET, // 未中奖
        REFUNDED,
        FAILED,
        ;// 失败

        public static STATUS me(String str){
            for (STATUS value : STATUS.values()) {
                if (value.name().equalsIgnoreCase(str)) {
                    return value;
                }
            }
            return null;
        }
    }

    @PrePersist
    public void prePersist() {
        this.updatedAt = new Date();
        this.createdAt = new Date();
        this.ticket = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

}
