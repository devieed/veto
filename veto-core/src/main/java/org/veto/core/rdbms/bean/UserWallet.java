package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.veto.shared.COIN_TYPE;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "user_wallets", indexes = {
        @Index(columnList = "balance"),
        @Index(columnList = "updated_at"),
        @Index(columnList = "coin_type"),
        @Index(columnList = "user_id, coin_type", unique = true),
        @Index(columnList = "flow_limit"),
        @Index(columnList = "total_bets")
})
@Setter
@Getter
public class UserWallet implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision =  18, scale = 2)
    private BigDecimal balance;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "coin_type")
    private COIN_TYPE coinType;

    private Boolean status;
    // 用户当前钱包的流水
    @Column(precision = 18, scale = 2)
    private BigDecimal flowLimit;
    /**
     * 用户累计收益
     */
    @Transient
    private BigDecimal reward;

    // 累计投注
    private BigDecimal totalBets;

    @PreUpdate
    public void preUpdate(){

        this.updatedAt = new Date();
    }

    @Version
    private Long version;
}
