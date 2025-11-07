package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.web3j.abi.datatypes.Bool;

import javax.swing.plaf.SpinnerUI;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "wallet_records", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "wallet_id"),
        @Index(columnList = "created_at"),
        @Index(columnList = "status"),
        @Index(columnList = "`before`"),
        @Index(columnList = "`after`"),
        @Index(columnList = "amount"),
        @Index(columnList = "`type`"),
        @Index(columnList = "is_blockchain")
})
@Setter
@Getter
public class WalletRecord implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Boolean isBlockchain;

    private Date createdAt;

    @Column(name = "`before`", precision = 18, scale = 2)
    private BigDecimal before;
    @Column(name = "`after`", precision = 18, scale = 2)
    private BigDecimal after;
    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    private Long walletId;

    private Boolean status;

    @Column(name = "`type`")
    @Enumerated(EnumType.ORDINAL)
    private TYPE type;

    @Column(precision = 18, scale = 2)
    private BigDecimal fee;

    @PrePersist
    public void prePersist(){
        this.createdAt = new Date();
    }

    public enum TYPE{
        RECHARGE, // 充值
        WITHDRAW, // 提现
        BET, // 下注
        RECOMMEND_REWARD, // 推荐奖励
        WITHDRAW_REFUND,
        BET_REWARD,// 下注收益
        BET_REFUND, // 下注退款
        FIRST_RECHARGE_BONUS, // 首充奖励
        INTEREST, // 利息
        ;

        public static TYPE me(String type){
            if (type == null){
                return null;
            }

            for (TYPE value : TYPE.values()) {
                if (value.name().equalsIgnoreCase(type)) {
                    return value;
                }
            }

            return null;
        }
    }
}
