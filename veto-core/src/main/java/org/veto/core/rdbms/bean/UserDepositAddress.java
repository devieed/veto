package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.veto.shared.COIN_TYPE;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户收款地址列表
 */
@Entity
@Table(name = "user_deposit_addresses", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "address", unique = true),
        @Index(columnList = "`coinType`")
})
@Setter
@Getter
public class UserDepositAddress implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;

    private Long userId;

    private COIN_TYPE coinType;

    private Date createdAt;

    @Column(precision = 18, scale = 2)
    private BigDecimal rechargeTotal;

    private Boolean status;
}
