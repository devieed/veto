package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户买入的比分下注及盈利率
 */
@Entity
@Table(name = "user_buy_odds", indexes = {
        @Index(columnList = "score_odds_id"),
        @Index(columnList = "user_id"),
        @Index(columnList = "purchase_odds"),
        @Index(columnList = "amount"),
        @Index(columnList = "status"),
        @Index(columnList = "created_at"),
        @Index(columnList = "ticket_date")
})
@Setter
@Getter
public class UserBuyOdds implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Date createdAt;

    @Column(name = "score_odds_id")
    private Long scoreOddsId;

    @JoinColumn(name = "score_odds_id", referencedColumnName = "id", updatable = false, insertable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ScoreOdds scoreOdds;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;
    // 赔付比例
    @Column(name = "purchase_odds", precision = 6, scale = 5)
    private BigDecimal purchaseOdds;

    @Enumerated(EnumType.ORDINAL)
    private ScoreOdds.STATUS status;
    // 开奖时间
    @Column(name = "ticket_date")
    private Date ticketDate;

    @PrePersist
    public void prePersist(){
        this.createdAt = new Date();
    }
}
