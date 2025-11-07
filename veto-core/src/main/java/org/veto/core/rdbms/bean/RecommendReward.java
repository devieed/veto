package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

import java.math.BigDecimal;
// 推荐奖励
@Entity
@Table(name = "recommend_reward", indexes = {
        @Index(columnList = "`level`", unique = true),
        @Index(columnList = "rate"),
        @Index(columnList = "status")
})
@Setter
@Getter
public class RecommendReward implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "rate", precision =  3, scale = 2)
    private BigDecimal rate;

    @Column(name = "`level`")
    private Integer level;

    private Boolean status;

    // 推荐层级报酬的付出者
    public enum FEE_PLAYER{
        BELOW, // 下级付出
        SYSTEM,
        ; // 系统付出

        public static FEE_PLAYER fromString(String s){
            if (s == null){
                return null;
            }
            for (FEE_PLAYER value : FEE_PLAYER.values()) {
                if (value.name().equalsIgnoreCase(s)) {
                    return value;
                }
            }

            return null;
        }
    }
}
